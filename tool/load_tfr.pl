#!/usr/bin/perl

#/*
# * FlightIntel
# *
# * Copyright 2020 Nadeem Hasan <nhasan@nadmm.com>
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
use warnings;
use HTML::LinkExtor;
use LWP::Simple;
use XML::Parser;

my $TFR_URL_BASE = "https://tfr.faa.gov/tfr2/";
my $TFR_URL = "$TFR_URL_BASE/list.html";
my $FEET_PER_METER = 3.28084;

my %links = ();
my $text = "";
my $NotUid = 0;
my $tfr_output = "";

my $valDistVerLower = "";
my $codeDistVerLower = "";
my $uomDistVerLower = "";
my $valDistVerUpper = "";
my $codeDistVerUpper = "";
my $uomDistVerUpper = "";
my $dateEffective = "";
my $dateExpire = "";
my $dateIndexYear = "";
my $noSeqNo = "";

sub cb {
    my($tag, %link) = @_;
    if ($tag eq "a") {
        my $href = $link{href};
        if ($href =~ /save_pages/) {
            $href =~ s/\.html/.xml/g;
            $links{$href}++;
        }
    }
}

my $LX = new HTML::LinkExtor(\&cb, $TFR_URL_BASE);

sub handle_start() {
    shift;
    my ($elem, %attrs) = @_;
    $text = "";
    if ($elem eq "NotUid") {
        $NotUid = 1;
    }
}

sub handle_char() {
    shift;
    my ($chars) = @_;
    $text .= $chars;
}

sub handle_end() {
    shift;
    my ($elem) = @_;

    if ($elem eq "TFRAreaGroup") {
        if ($uomDistVerLower eq "M") {
            $valDistVerLower *= $FEET_PER_METER;
        }
        elsif ($uomDistVerLower eq "FL") {
            $valDistVerLower *= 100;
        }
        if ($codeDistVerLower eq "HEI") {
            $codeDistVerLower = "A";
        }
        elsif ($codeDistVerLower eq "ALT") {
            $codeDistVerLower = "M";
        }
        $tfr_output .= "  <MINALT>${valDistVerLower}${codeDistVerLower}</MINALT>\n";

        if ($uomDistVerUpper eq "M") {
            $valDistVerUpper *= $FEET_PER_METER;
        }
        elsif ($uomDistVerUpper eq "FL") {
            $valDistVerUpper *= 100;
        }
        if ($codeDistVerUpper eq "HEI") {
            $codeDistVerUpper = "A";
        }
        elsif ($codeDistVerUpper eq "ALT") {
            $codeDistVerUpper = "M";
        }
        $tfr_output .= "  <MAXALT>${valDistVerUpper}${codeDistVerUpper}</MAXALT>\n";

        $valDistVerLower = "";
        $codeDistVerLower = "";
        $uomDistVerLower = "";
        $valDistVerUpper = "";
        $codeDistVerUpper = "";
        $uomDistVerUpper = "";

        $tfr_output .= "  <ACTIVE><TEXT>${dateEffective}Z</TEXT></ACTIVE>\n";
        if ($dateExpire ne "") {
            $tfr_output .= "  <EXPIRES><TEXT>${dateExpire}Z</TEXT></EXPIRES>\n";
        }

        $dateEffective = "";
        $dateExpire = "";
    }
    elsif ($elem eq "NotUid") {
        $NotUid = 0;
    }
    elsif ($elem eq "dateEffective") {
        $dateEffective = $text;
    }
    elsif ($elem eq "dateExpire") {
        $dateExpire = $text;
    }
    elsif ($elem eq "valDistVerLower") {
        $valDistVerLower = $text;
    }
    elsif ($elem eq "codeDistVerLower") {
        $codeDistVerLower = $text;
    }
    elsif ($elem eq "uomDistVerLower") {
        $uomDistVerLower = $text;
    }
    elsif ($elem eq "valDistVerUpper") {
        $valDistVerUpper = $text;
    }
    elsif ($elem eq "codeDistVerUpper") {
        $codeDistVerUpper = $text;    
    }
    elsif ($elem eq "uomDistVerUpper") {
        $uomDistVerUpper = $text;
    }
    elsif ($elem eq "dateIssued" && $NotUid) {
        $tfr_output .= "  <CREATED><TEXT>${text}Z</TEXT></CREATED>\n";
        $tfr_output .= "  <MODIFIED><TEXT>${text}Z</TEXT></MODIFIED>\n";
    }
    elsif ($elem eq "dateIndexYear" && $NotUid) {
        $dateIndexYear = $text;
    }
    elsif ($elem eq "noSeqNo" && $NotUid) {
        $noSeqNo = $text;
    }
    elsif ($elem eq "txtLocalName" && $NotUid) {
        $tfr_output .= "  <NAME>$text</NAME>\n";
    }
    elsif ($elem eq "txtDescrUSNS") {
        $tfr_output .= "  <SRC>$text</SRC>\n";
    }
    elsif ($elem eq "Group") {
        $dateIndexYear %= 10;
        $tfr_output .= "  <NID>${dateIndexYear}/${noSeqNo}</NID>\n";
    }
    elsif ($elem eq "codeFacility") {
        $tfr_output .= "  <FACILITY>$text</FACILITY>\n";
    }
    elsif ($elem eq "txtNameCity") {
        $tfr_output .= "  <CITY>$text</CITY>\n";
    }
    elsif ($elem eq "txtNameUSState") {
        $tfr_output .= "  <STATE>$text</STATE>\n";
    }
    elsif ($elem eq "codeCoordFacilityType") {
        $tfr_output .= "  <FACILITYTYPE>$text</FACILITYTYPE>\n";
    }
}

sub handle_default() {
}

my $parser = new XML::Parser (Handlers => {
                              Start   => \&handle_start,
                              End     => \&handle_end,
                              Char    => \&handle_char,
                              Default => \&handle_default,
                            });

my $tfr_html = get($TFR_URL) or die;

$LX->parse($tfr_html);

$tfr_output = "<xml>\n";

my $seq = "0";

foreach my $url (keys %links) {
    print "$url\n";
    $tfr_output .= "<TFR$seq>\n";
    my $xml = get($url);
    $parser->parse($xml);
    $tfr_output .= "</TFR>\n";
    $seq++;
    last;
}

$tfr_output .= "</xml>";

print "$tfr_output\n";