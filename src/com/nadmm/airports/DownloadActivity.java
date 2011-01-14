/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.nadmm.airports;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

public final class DownloadActivity extends ListActivity {
    static final String TAG = DownloadActivity.class.getName();
    static final String HOST = "10.0.2.2";
    static final Integer PORT = 80;
    static final String PATH = "/~nhasan/fadds";
    static final String FILE = "fadds_20101118.bz2";
    static final String MANIFEST = "manifest.xml";

    private final class DataFileInfo {
        public String type;
        public String desc;
        public int version;
        public String fileName;
        public int size;
        public Date start;
        public Date end;

        public DataFileInfo() {
        }

        public DataFileInfo(DataFileInfo info) {
            type = info.type;
            desc = info.desc;
            version = info.version;
            fileName = info.fileName;
            size = info.size;
            start = info.start;
            end = info.end;
        }
    }

    private final ArrayList<DataFileInfo> mInstalledData = new ArrayList<DataFileInfo>();
    private final ArrayList<DataFileInfo> mAvailableData = new ArrayList<DataFileInfo>();

    private DownloadListAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new DownloadListAdapter(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	setTitle( "Airports - "+getTitle() );
        checkData();
    }

    private void checkData() {
        CheckDataTask task = new CheckDataTask(this);
        task.execute();
    }

    private void download() {
        DownloadTask task = new DownloadTask(this);
        task.execute();
    }

    private final class DownloadListAdapter extends BaseAdapter {
    	private LayoutInflater mInflater;
    	private final DateFormat mDateFormat = DateFormat.getDateInstance();
    	private Context mContext;
    	private final int VIEW_TYPE_SECTION_HEADER = 0;
    	private final int VIEW_TYPE_DATA_FILE = 1;

		public DownloadListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			mContext = context;
		}

		private int getInstalledCount() {
			return mInstalledData.isEmpty()? 1: mInstalledData.size();
		}

		private int getAvailableCount() {
			return mAvailableData.isEmpty()? 1: mAvailableData.size();
		}

		private int getInstalledHeaderPos() {
			return 0;
		}

		private int getAvailableHeaderPos() {
			return getInstalledCount()+1;
		}

		private int getInstalledItemOffset() {
			return getInstalledHeaderPos()+1;
		}

		private int getAvailableItemOffset() {
			return getAvailableHeaderPos()+1;
		}

		@Override
		public int getCount() {
			// Account for 2 section headers
			return 2+getInstalledCount()+getAvailableCount();
		}

		@Override
		public Object getItem(int position) {
			Log.v(TAG, "getItem("+position+")");
			return position;
		}

		@Override
		public long getItemId(int position) {
			Log.v(TAG, "getItemId("+position+")");
			return position;
		}

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public int getViewTypeCount() {
        	// We have 2 view types: section and data
        	return 2;
        }

        @Override public int getItemViewType(int position) {
        	return ( position == getInstalledHeaderPos() || position == getAvailableHeaderPos() )?
        			VIEW_TYPE_SECTION_HEADER : VIEW_TYPE_DATA_FILE;
        }

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {
			int viewType = getItemViewType(position);
			Log.v(TAG, "position="+position+"; viewType="+viewType);

			if (viewType == VIEW_TYPE_SECTION_HEADER) {
				if ( convertView == null ) {
					convertView = mInflater.inflate( R.layout.download_list_section, null );
				}

				TextView section = (TextView) convertView.findViewById(R.id.download_section);
				if (position == 0) {
					section.setText(R.string.download_installed);
				}
				else {
					section.setText(R.string.download_available);
				}
			}
			else {
				if (position>=getInstalledHeaderPos() && position<getAvailableHeaderPos()) {
					if (mInstalledData.isEmpty()) {
						if ( convertView == null ) {
							convertView = mInflater.inflate( R.layout.download_list_nodata, null );
						}
						TextView msg = (TextView) convertView.findViewById(R.id.download_msg);
						msg.setText(R.string.download_noinstall);
					}
					else {
						if ( convertView == null ) {
							convertView = mInflater.inflate( R.layout.download_list_item, null );
						}
						DataFileInfo info = mInstalledData.get(position-getInstalledItemOffset());
						TextView desc = (TextView) convertView.findViewById( R.id.download_desc );
						desc.setText( info.desc );
						TextView dates = (TextView) convertView.findViewById(R.id.download_dates);
						dates.setText( "Effective from "+mDateFormat.format(info.start)+" to "
								+mDateFormat.format(info.end));
						TextView size = (TextView) convertView.findViewById(R.id.download_size);
						size.setText("Current");
					}
				}
				else {
					if (mAvailableData.isEmpty()) {
						if ( convertView == null ) {
							convertView = mInflater.inflate(R.layout.download_list_nodata, null);
						}
						TextView msg = (TextView) convertView.findViewById(R.id.download_msg);
						msg.setText(R.string.download_noupdate);
					}
					else {
						if ( convertView == null ) {
							convertView = mInflater.inflate( R.layout.download_list_item, null );
						}						
						DataFileInfo info = mAvailableData.get(position-getAvailableItemOffset());
						TextView desc = (TextView) convertView.findViewById( R.id.download_desc );
						desc.setText( info.desc );
						TextView dates = (TextView) convertView.findViewById(R.id.download_dates);
						dates.setText("Effective "+DateUtils.formatDateRange(mContext, 
								info.start.getTime(), info.end.getTime(), 
								DateUtils.FORMAT_SHOW_YEAR|DateUtils.FORMAT_ABBREV_ALL));
						TextView size = (TextView) convertView.findViewById(R.id.download_size);
						size.setText(Formatter.formatShortFileSize(mContext, info.size)
								+"   ("+DateUtils.formatElapsedTime(info.size/(500*1024/8))
								+" @ 500kbps)");
					}
				}
			}

			return convertView;
		}    	
    }

    private final class CheckDataTask extends AsyncTask<Void, Void, Integer> {
        private DownloadActivity mActivity;

        public CheckDataTask( DownloadActivity activity ) {
            mActivity = activity;
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpHost target = new HttpHost( HOST, PORT );
                URI uri = new URI( PATH+"/"+MANIFEST );
                HttpGet get = new HttpGet( uri );

                HttpResponse response = httpClient.execute( target, get );

                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    Log.v( TAG, response.getStatusLine().getReasonPhrase() );
                    return -2;
                }

                HttpEntity entity = response.getEntity();
                InputStream in = entity.getContent();
                FileOutputStream out = mActivity.openFileOutput( MANIFEST, MODE_PRIVATE );

                byte[] buffer = new byte[ 16384 ];
                int len = buffer.length;
                int count = 0;

                while ( ( count = in.read( buffer, 0, len ) ) != -1 ) {
                    out.write( buffer, 0, count );
                }

                in.close();
                out.close();
            }
            catch ( IOException e ) {
                Log.v( TAG, "IOException: "+e.getMessage() );
                return -1;
            }
            catch ( URISyntaxException e ) {
                Log.v( TAG, "URISyntaxException: "+e.getMessage() );
                return -1;
            }

            Log.v( TAG, "Got manifest XML file" );
            return 0;
        }

        @Override
        protected void onPreExecute() {
        	setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute( Integer result ) {
            if ( result != 0 ) {
            	setProgressBarIndeterminateVisibility(false);
                showMessage( "Download", "There was an error while cheking for updates" );
                return;
            }

            FileInputStream in;
            try {
                in = mActivity.openFileInput( MANIFEST );
            } catch ( FileNotFoundException e ) {
                Log.v( TAG, e.getMessage() );
            	setProgressBarIndeterminateVisibility(false);
                showMessage( "Download", "Unable to read manifest file" );
                return;
            }

            parseManifest( in );
        	setProgressBarIndeterminateVisibility(false);

        	mActivity.setListAdapter(mAdapter);
        }

        private void parseManifest( InputStream in ) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
            final DataFileInfo info = new DataFileInfo();
            RootElement root = new RootElement( "manifest" );
            Element datafile = root.getChild( "datafile" );
            datafile.setEndElementListener( new EndElementListener() {
                @Override
                public void end() {
                    mAvailableData.add( new DataFileInfo( info ) );
                }
            } );
            datafile.getChild( "type" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    info.type = body;
                }
            } );
            datafile.getChild( "desc" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    info.desc = body;
                }
            } );
            datafile.getChild( "version" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    info.version = Integer.parseInt( body );
                }
            } );
            datafile.getChild( "filename" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    info.fileName = body;
                }
            } );
            datafile.getChild( "size" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    info.size = Integer.parseInt( body );
                }
            } );
            datafile.getChild( "start" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end( String body ) {
                    try {
                        info.start = dateFormat.parse( body );
                    } catch ( ParseException e ) {
                        Log.v( TAG, "Error parsing start date: "+e.getMessage() );
                    }
                }
            } );
            datafile.getChild( "end" ).setEndTextElementListener( new EndTextElementListener() {
                @Override
                public void end(String body) {
                    try {
                        info.end = dateFormat.parse( body );
                    } catch ( ParseException e ) {
                        Log.v( TAG, "Error parsing end date: "+e.getMessage() );
                    }
                }
            } );

            try {
                Xml.parse( in, Xml.Encoding.UTF_8, root.getContentHandler() );
            }
            catch ( Exception e ) {
                Log.v( TAG, "Error parsing manifest: "+e.getMessage() );
            }

            Iterator<DataFileInfo> it = mAvailableData.iterator();
            while ( it.hasNext() )
            {
                DataFileInfo dataFile = it.next();
                Log.v( TAG, "==== Data File Info ====" );
                Log.v( TAG, "type="+dataFile.type );
                Log.v( TAG, "desc="+dataFile.desc );
                Log.v( TAG, "version="+dataFile.version );
                Log.v( TAG, "filename="+dataFile.fileName );
                Log.v( TAG, "size="+dataFile.size );
                Log.v( TAG, "start="+dataFile.start.toLocaleString() );
                Log.v( TAG, "end="+dataFile.end.toLocaleString() );
            }
        }
    }

    private final class DownloadTask extends AsyncTask<Void, Integer, Integer> {
        private ProgressDialog mProgressDialog;
        private Context mContext;

        public DownloadTask( Context context )
        {
            mContext = context;
        }

        @Override
        protected Integer doInBackground( Void... params ) {
            try {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpHost target = new HttpHost( HOST, PORT );
                URI uri = new URI( PATH+"/"+FILE );
                HttpGet get = new HttpGet( uri );

                HttpResponse response = httpClient.execute( target, get );

                if ( response.getStatusLine().getStatusCode() != 200 ) {
                    Log.v( TAG, response.getStatusLine().getReasonPhrase() );
                    return -2;
                }

                HttpEntity entity = response.getEntity();

                mProgressDialog.setMax( (int) entity.getContentLength() );

                InputStream input = entity.getContent();
                FileOutputStream output = mContext.openFileOutput( FILE, MODE_PRIVATE );

                Log.v(TAG, "Opened file for writing" );

                byte[] buffer = new byte[ 16384 ];
                int len = buffer.length;
                int count = 0;

                while ( ( count = input.read( buffer, 0, len ) ) != -1 ) {
                    output.write( buffer, 0, count );
                    publishProgress( count );
                }

                input.close();
                output.close();
            }
            catch ( IOException e ) {
                Log.v( TAG, "IOException: "+e.getMessage() );
                return -1;
            }
            catch ( URISyntaxException e ) {
                Log.v( TAG, "URISyntaxException: "+e.getMessage() );
                return -1;
            }

            return 0;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog( mContext );
            mProgressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
            mProgressDialog.setMessage( "Downloading data..." );
            mProgressDialog.setProgress( 0 );
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate( Integer... progress ) {
            mProgressDialog.incrementProgressBy( progress[ 0 ] );
        }

        @Override
        protected void onPostExecute( Integer result ) {
            mProgressDialog.dismiss();
            if ( result == 0 ) {
                showMessage( "Download", "The data was downloaded successfully" );
            }
            else if ( result == -1 ) {
                showMessage( "Download", "There was an error while downloading data" );
            }
            else if ( result == -2 ) {
                showMessage( "Download", "Data file was not found on the server" );
            }
        }
    }

    protected void showMessage( String title, String msg ) {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( msg )
               .setTitle( title )
               .setPositiveButton( "Close", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                   }
               } );
        AlertDialog alert = builder.create();
        alert.show();
    }
}
