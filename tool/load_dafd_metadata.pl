#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2012-2019 Nadeem Hasan <nhasan@nadmm.com>
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

my $BASE_DIR = shift @ARGV;
our $cycle = shift @ARGV;
my $AFD_METADATA_FILE = glob "${BASE_DIR}/afd_*.xml";
my $count = 0;

my $dbfile = "dcs_$cycle.db";
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
        ."AFD_CYCLE TEXT, "
        ."FROM_DATE TEXT, "
        ."TO_DATE TEXT"
        .")";

my $insert_cycle_record = "INSERT INTO cycle ("
        ."AFD_CYCLE, "
        ."FROM_DATE, "
        ."TO_DATE"
        .") VALUES ("
        ."?, ?, ?"
        .")";

my $create_dafd_table = "CREATE TABLE dafd ("
        ."_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        ."STATE TEXT, "
        ."FAA_CODE TEXT, "
        ."PDF_NAME TEXT"
        .")";

my $insert_dafd_record = "INSERT INTO dafd ("
        ."STATE, "
        ."FAA_CODE, "
        ."PDF_NAME"
        .") VALUES ("
        ."?, ?, ?"
        .")";

$dbh->do( "DROP TABLE IF EXISTS cycle" );
$dbh->do( $create_cycle_table );
my $sth_cycle = $dbh->prepare( $insert_cycle_record );

$dbh->do( "DROP TABLE IF EXISTS dafd" );
$dbh->do( $create_dafd_table );
$dbh->do( "CREATE INDEX idx_dafd_faa_code on dafd ( FAA_CODE );" );
my $sth_dafd = $dbh->prepare( $insert_dafd_record );

my $twig= new XML::Twig(
                        start_tag_handlers => {
                            airports => \&airports,
                            location => \&location },
                        twig_handlers => {
                            airport => \&airport } );

$twig->parsefile( $AFD_METADATA_FILE );

print "\rDone loading $count records\n";

exit;

my $from_date;
my $to_date;
my $faa_code;
my $state;

sub airports
{
    my( $twig, $dafd )= @_;
    my $from_date = $dafd->{'att'}->{'from_edate'};
    my $to_date = $dafd->{'att'}->{'to_edate'};

    #AFD_CYCLE
    $sth_cycle->bind_param( 1, $main::cycle );
    #FROM_DATE
    $sth_cycle->bind_param( 2, $from_date );
    #TO_DATE
    $sth_cycle->bind_param( 3, $to_date );

    $sth_cycle->execute;

    return 1;
}

sub location
{
    my( $twig, $location )= @_;
    $state = $location->{'att'}->{'state'};
}

sub airport
{
    my( $twig, $airport )= @_;
    my $faa_code = $airport->child_text( 0, "aptid" );
    my $pdf_name = $airport->child_text( 0, "pdf" );

    if ( length( $faa_code ) > 0 )
    {
        #STATE
        $sth_dafd->bind_param( 1, $state );
        #FAA_CODE
        $sth_dafd->bind_param( 2, $faa_code );
        #PDF_NAME
        $sth_dafd->bind_param( 3, $pdf_name );

        print "\rLoading # $count...";

        $sth_dafd->execute;

        $twig->purge;
        ++$count;
    }

    return 1;
}
