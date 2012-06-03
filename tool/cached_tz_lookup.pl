#!/usr/bin/perl

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

my $FADDS_BASE = shift @ARGV;
# Current database
my $dbfile1 = shift @ARGV;
# Previous database
my $dbfile2 = shift @ARGV;

my $dbh1 = DBI->connect( "dbi:SQLite:dbname=$FADDS_BASE/$dbfile1", "", "" );
my $dbh2 = DBI->connect( "dbi:SQLite:dbname=$FADDS_BASE/$dbfile2", "", "" );

my $sth_upd = $dbh1->prepare( "update airports set TIMEZONE_ID=? where SITE_NUMBER=?" );

my $row = 0;
my $site_number;
my $tz;

my $sth = $dbh2->prepare( "select SITE_NUMBER, TIMEZONE_ID from airports" );
$sth->execute or die "Can't execute statement: $DBI::errstr\n";

while ( ( $site_number, $tz ) = $sth->fetchrow_array )
{
    print "Cached row=[$row], site=[$site_number], tz=[$tz]\n";
    $sth_upd->bind_param( 1, $tz );
    $sth_upd->bind_param( 2, $site_number );
    $sth_upd->execute();
    ++$row;
}

$dbh1->disconnect();
$dbh2->disconnect();

exit;
