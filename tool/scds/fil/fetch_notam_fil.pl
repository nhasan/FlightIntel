#! /usr/bin/perl

#/*
# * FlightIntel
# *
# * Copyright 2021 Nadeem Hasan <nhasan@nadmm.com>
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

# Ubuntu package dependencies
#   libconfig-simple-perl
#   libnet-sftp-foreign-perl
#

use strict;
use warnings;
use v5.10;

use Config::Simple;
use Net::SFTP::Foreign;
use File::Copy qw(mv);
use File::Path qw(make_path);

my $cfgfile = shift or die "Missing config file parameter.";
-f $cfgfile or die "Config file not found.";
my $cfg = new Config::Simple($cfgfile) or die Config::Simple->error();

my $SWIM = $cfg->param(-block => "SWIM");
my $host = $SWIM->{host};
my $user = $SWIM->{user};

my $FIL = $cfg->param(-block => "FIL");
my $timestampfile = $FIL->{timestampfile};
my $localtimestamp = $FIL->{timestamp} // "";
my $outdir = $FIL->{outdir};
my $tmpdir = $FIL->{tmpdir};
my $datafile = $FIL->{datafile};

my $retry = 5;      
my $error;

# Create the directories if missing
-e $outdir || make_path($outdir);
-e $tmpdir || make_path($tmpdir);

while ($retry) {
    $error = 0;
    say "Connecting to $user\@$host...";
    my $sftp = Net::SFTP::Foreign->new($host, user => $user, queue_size => 1) or $error = 1;
    if ($error) { 
        say "SFTP connection failed: ".sftp->error;
        sleep(30);
        $retry--;
        next;
    }
    say "Created SFTP session.";
    my $remotetimestamp = $sftp->get_content($timestampfile) or $error = 1;
    if ($error) {
        say "$timestampfile failed: ".$sftp->error;
        undef $sftp;
        sleep(30);
        $retry--;
        next;
    }

    chomp($remotetimestamp);

    say "Last fetch timestamp was $localtimestamp";
    if ($localtimestamp ne $remotetimestamp) {
        say "Fetching data file $datafile from FAA server.";
        $sftp->get($datafile, "$tmpdir/$datafile") or $error = 1;
        if ($error) {
            say "$datafile failed: ".$sftp->error;
            undef $sftp;
            sleep(30);
            $retry--;
            next;
        }

        mv("$tmpdir/$datafile", "$outdir/$datafile");

        $FIL->{timestamp} = $remotetimestamp;
        $cfg->param(-block => "FIL", -values => $FIL);
        $cfg->save();
    } else {
        say "Noting new to fetch from FAA server.";
    }

    say "Closing SFTP session.";
    undef $sftp;
    last;
}
