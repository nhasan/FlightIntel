#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
# *
# * This program is free software: you can redistribute it and/or modify
# * it under the terms of the GNU General Public License as published by
# * the Free Software Foundation, either version 3 of the License, or
# * (at your option) any later version.
# *
# * This program is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
# */

use strict;
use DBI;
use LWP::Simple;
use XML::Twig;

my $BASE_DIR=shift @ARGV;
my $cycle = shift @ARGV;
my $TPP_METADATA_FILE="${BASE_DIR}/d-TPP_Metafile.xml";
#my $dtpp_url = "http://aeronav.faa.gov/d-tpp/$cycle/xml_data/d-TPP_Metafile.xml";
#my $dtpp_url = "https://nfdc.faa.gov/webContent/dtpp/current.xml";
my $dtpp_url = "file://data/d-TPP/DDTPPE_${cycle}/${TPP_METADATA_FILE}";
my $count = 0;

my $ofh = select STDOUT;
$| = 1;
select $ofh;

print "Using ${TPP_METADATA_FILE}\n";
#print "Downloading the d-TPP metafile: ".$dtpp_url."...";
#my $ret = getstore( $dtpp_url, "d-TPP_Metafile.xml");
#if ( $ret != 200 )
#{
#    die "\nERROR: Unable to download d-TPP metadata. HTTP-$ret\n\n";
#}
#print "done\n";

my $dbfile = "dtpp_".$cycle.".db";
my $dbh = DBI->connect( "dbi:SQLite:dbname=$dbfile", "", "" );

$dbh->do( "PRAGMA page_size=4096" );
$dbh->do( "PRAGMA synchronous=OFF" );

my $create_metadata_table = "CREATE TABLE android_metadata ( locale TEXT );";
my $insert_metadata_record = "INSERT INTO android_metadata VALUES ( 'en_US' );";

$dbh->do( "DROP TABLE IF EXISTS android_metadata" );
$dbh->do( $create_metadata_table );
$dbh->do( $insert_metadata_record );

my $create_cycle_table = "CREATE TABLE cycle ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."TPP_CYCLE TEXT, "
        ."FROM_DATE TEXT, "
        ."TO_DATE TEXT"
        .")";

my $insert_cycle_record = "INSERT INTO cycle ("
        ."TPP_CYCLE, "
        ."FROM_DATE, "
        ."TO_DATE"
        .") VALUES ("
        ."?, ?, ?"
        .")";

my $create_dtpp_table = "CREATE TABLE dtpp ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."TPP_VOLUME TEXT, "
        ."FAA_CODE TEXT, "
        ."CHART_SEQ TEXT, "
        ."CHART_CODE TEXT, "
        ."CHART_NAME TEXT, "
        ."USER_ACTION TEXT, "
        ."PDF_NAME TEXT, "
        ."FAANFD18_CODE TEXT, "
        ."MILITARY_USE TEXT, "
        ."COPTER_USE TEXT"
        .")";

my $insert_dtpp_record = "INSERT INTO dtpp ("
        ."TPP_VOLUME, "
        ."FAA_CODE, "
        ."CHART_SEQ, "
        ."CHART_CODE, "
        ."CHART_NAME, "
        ."USER_ACTION, "
        ."PDF_NAME, "
        ."FAANFD18_CODE, "
        ."MILITARY_USE, "
        ."COPTER_USE"
        .") VALUES ("
        ."?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS dtpp" );
$dbh->do( $create_dtpp_table );
$dbh->do( "CREATE INDEX idx_dtpp_faa_code on dtpp ( FAA_CODE );" );
my $sth_dtpp = $dbh->prepare( $insert_dtpp_record );

$dbh->do( "DROP TABLE IF EXISTS cycle" );
$dbh->do( $create_cycle_table );
my $sth_cycle = $dbh->prepare( $insert_cycle_record );

my $twig= new XML::Twig(
                        start_tag_handlers => { 
                            digital_tpp => \&digital_tpp,
                            city_name => \&city_name,
                            airport_name => \&airport_name },
                        twig_handlers => {
                            record => \&record } );

$twig->parsefile( $TPP_METADATA_FILE );

print "\rDone loading $count records\n";

exit;

my $cycle;
my $from_date;
my $to_date;
my $volume;
my $faa_code;
my $military_use;

sub digital_tpp
{
    my( $twig, $dtpp )= @_;
    my $cycle = $dtpp->{'att'}->{'cycle'};
    my $from_date = $dtpp->{'att'}->{'from_edate'};
    my $to_date = $dtpp->{'att'}->{'to_edate'};

    #TPP_CYCLE
    $sth_cycle->bind_param( 1, $cycle );
    #FROM_DATE
    $sth_cycle->bind_param( 2, $from_date );
    #TO_DATE
    $sth_cycle->bind_param( 3, $to_date );

    $sth_cycle->execute;

    return 1;
}

sub city_name
{
    my( $twig, $city )= @_;
    $volume = $city->{'att'}->{'volume'};
    return 1;
}

sub airport_name
{
    my( $twig, $apt )= @_;
    $faa_code = $apt->{'att'}->{'apt_ident'};
    $military_use = $apt->{'att'}->{'military'};
    return 1;
}

sub record
{
    my( $twig, $record )= @_;
    my $chart_seq = $record->child_text( 0, "chartseq" );
    my $chart_code = $record->child_text( 0, "chart_code" );
    my $chart_name = $record->child_text( 0, "chart_name" );
    my $user_action = $record->child_text( 0, "useraction" );
    my $pdf_name = $record->child_text( 0, "pdf_name" );
    my $faanfd18_code = $record->child_text( 0, "faanfd18" );
    my $copter_use = $record->child_text( 0, "copter" );

    #TPP_VOLUME
    $sth_dtpp->bind_param( 1, $volume );
    #FAA_CODE
    $sth_dtpp->bind_param( 2, $faa_code );
    #CHART_SEQ
    $sth_dtpp->bind_param( 3, $chart_seq );
    #CHART_CODE
    $sth_dtpp->bind_param( 4, $chart_code );
    #CHART_NAME
    $sth_dtpp->bind_param( 5, $chart_name );
    #USER_ACTION
    $sth_dtpp->bind_param( 6, $user_action );
    #PDF_NAME
    $sth_dtpp->bind_param( 7, $pdf_name );
    #FAANFD18_CODE
    $sth_dtpp->bind_param( 8, $faanfd18_code );
    #MILITARY_USE
    $sth_dtpp->bind_param( 9, $military_use );
    #COPTER_USE
    $sth_dtpp->bind_param( 10, $copter_use );

    print "\rLoading # $count...";

    $sth_dtpp->execute;

    $twig->purge;
    ++$count;

    return 1;
}
