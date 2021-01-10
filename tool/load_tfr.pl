#!/usr/bin/perl

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
#    libhtml-linkextractor-perl
#    liblwp-protocol-https-perl
#    libtext-autoformat-perl
#    libxml-parser-perl

use strict;
use warnings;
use POSIX;
use HTML::LinkExtor;
use LWP::Simple;
use Text::Autoformat;
use XML::Parser;

my $TFR_URL_BASE = "https://tfr.faa.gov/tfr2/";
my $TFR_URL = "$TFR_URL_BASE/list.html";
my $FEET_PER_METER = 3.28084;
my $TFR_OUTPUT_FILE = "/var/www/api.flightintel.com/html/data/tfr_list.xml";

my %links = ();
my @tfr_list = ();

# Order of strings is important
my @tfr_types = ("Special", "Security", "Space Operations", "VIP", "Hazards");

my $tfr_output;
my @valDistVerLower;
my @codeDistVerLower;
my @uomDistVerLower;
my @valDistVerUpper;
my @codeDistVerUpper;
my @uomDistVerUpper;
my $dateIndexYear;
my $noSeqNo;
my $TFRAreaGroup;
my $text;
my $numGroups;

my $reTrim = qr/^\s+|\s+$/;

sub capitalize($)
{
    my ( $string ) = @_;
    $string = autoformat( $string, { case => 'highlight' } );
    $string =~ s/\s+(Afb)\s+/ \U$1\E /g;
    $string =~ s/$reTrim//g;
    return $string;
}

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

    if ($elem eq "Not") {
        $tfr_output = "";
        $tfr_output .= "  <TFR>\n";
    }
    elsif ($elem eq "TFRAreaGroup") {
        $TFRAreaGroup = 1;
    }
    elsif ($elem eq "TfrNot") {
        @valDistVerLower = ();
        @codeDistVerLower = ();
        @uomDistVerLower = ();
        @valDistVerUpper = ();
        @codeDistVerUpper = ();
        @uomDistVerUpper = ();
        $dateIndexYear = "";
        $noSeqNo = "";
        $TFRAreaGroup = 0;
        $numGroups = 0;
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
        $TFRAreaGroup = 0;
        $numGroups++;
    }
    elsif ($elem eq "NotUid") {
        $tfr_output .= "    <NID>${dateIndexYear}/${noSeqNo}</NID>\n";
    }
    elsif ($elem eq "dateEffective" && !$TFRAreaGroup) {
        $tfr_output .= "    <ACTIVE>${text}Z</ACTIVE>\n";
    }
    elsif ($elem eq "dateExpire" && !$TFRAreaGroup) {
        $tfr_output .= "    <EXPIRES>${text}Z</EXPIRES>\n";
    }
    elsif ($elem eq "valDistVerLower") {
        push @valDistVerLower, $text;
    }
    elsif ($elem eq "codeDistVerLower") {
        push @codeDistVerLower, $text;
    }
    elsif ($elem eq "uomDistVerLower") {
        push @uomDistVerLower, $text;
    }
    elsif ($elem eq "valDistVerUpper") {
        push @valDistVerUpper, $text;
    }
    elsif ($elem eq "codeDistVerUpper") {
        push @codeDistVerUpper, $text;    
    }
    elsif ($elem eq "uomDistVerUpper") {
        push @uomDistVerUpper, $text;
    }
    elsif ($elem eq "dateIssued") {
        $tfr_output .= "    <CREATED>${text}Z</CREATED>\n";
        $tfr_output .= "    <MODIFIED>${text}Z</MODIFIED>\n";
    }
    elsif ($elem eq "dateIndexYear") {
        $dateIndexYear = $text % 10;
    }
    elsif ($elem eq "noSeqNo") {
        $noSeqNo = $text;
    }
    elsif ($elem eq "txtLocalName") {
        $tfr_output .= "    <NAME>$text</NAME>\n";
    }
    elsif ($elem eq "txtDescrUSNS") {
        $tfr_output .= "    <SRC>$text</SRC>\n";
    }
    elsif ($elem eq "txtDescrModern" ) {
        foreach my $tfr_type (@tfr_types) {
            if ($text =~ /\>$tfr_type\</) {
                $tfr_output .= "    <TYPE>$tfr_type</TYPE>\n";
                last;
            }
        }
    }
    elsif ($elem eq "codeFacility") {
        $tfr_output .= "    <FACILITY>$text</FACILITY>\n";
    }
    elsif ($elem eq "txtNameCity") {
        $tfr_output .= "    <CITY>".capitalize($text)."</CITY>\n";
    }
    elsif ($elem eq "txtNameUSState") {
        $tfr_output .= "    <STATE>".capitalize($text)."</STATE>\n";
    }
    elsif ($elem eq "codeCoordFacilityType") {
        $tfr_output .= "    <FACILITYTYPE>$text</FACILITYTYPE>\n";
    }
    elsif ($elem eq "TfrNot") {
        # Normalize the height values and decode AGL/MSL
        my $index = 0;
        while ($index < $numGroups) {
            my $uom = $uomDistVerLower[$index];
            my $code = $codeDistVerLower[$index];
            my $val = $valDistVerLower[$index];

            if ($uomDistVerLower[$index] eq "M") {
                $valDistVerLower[$index] *= $FEET_PER_METER;
            }
            elsif ($uomDistVerLower[$index] eq "FL") {
                $valDistVerLower[$index] *= 100;
            }
            $codeDistVerLower[$index] = "ALT" if !defined $codeDistVerLower[$index]; 
            if ($codeDistVerLower[$index] eq "HEI") {
                $codeDistVerLower[$index] = "A";
            }
            elsif ($codeDistVerLower[$index] eq "ALT") {
                $codeDistVerLower[$index] = "M";
            }
            if ($uomDistVerUpper[$index] eq "M") {
                $valDistVerUpper[$index] *= $FEET_PER_METER;
            }
            elsif ($uomDistVerUpper[$index] eq "FL") {
                $valDistVerUpper[$index] *= 100;
            }
            $codeDistVerUpper[$index] = "ALT" if !defined $codeDistVerUpper[$index]; 
            if ($codeDistVerUpper[$index] eq "HEI") {
                $codeDistVerUpper[$index] = "A";
            }
            elsif ($codeDistVerUpper[$index] eq "ALT") {
                $codeDistVerUpper[$index] = "M";
            }
            $index++;
        }

        my $codeLower;
        my $valLower;
        my $codeUpper;
        my $valUpper; 

        # Calculate min and max heights across all groups
        $index = 0;
        while ($index < $numGroups) {
            if (!defined $valLower or $valDistVerLower[$index] < $valLower) {
                $valLower = $valDistVerLower[$index];
                $codeLower = $codeDistVerLower[$index];
            }
            
            if (!defined $valUpper or $valDistVerUpper[$index] > $valUpper) {
                $valUpper = $valDistVerUpper[$index];
                $codeUpper = $codeDistVerUpper[$index];
            }
            $index++;
        }

        $tfr_output .= "    <MINALT>${valLower}${codeLower}</MINALT>\n";
        $tfr_output .= "    <MAXALT>${valUpper}${codeUpper}</MAXALT>\n";
    }
    elsif ($elem eq "Not") {
        $tfr_output .= "  </TFR>\n";
        push @tfr_list, $tfr_output;
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

my $seq = 0;

foreach my $url (keys %links) {
    if (my $xml = get($url) ) {
        print "Fetched $url\n";
        $seq++;
        $parser->parse($xml);
    } else {
        print "Failed  $url\n";
    }
}

my $now = strftime "%FT%TZ", gmtime time;
$tfr_output = "";
$tfr_output = "<?xml version=\"1.0\" ?>\n";
$tfr_output .= "<TFRList count=\"$seq\" timestamp=\"$now\">\n";

foreach my $tfr (@tfr_list) {
    $tfr_output .= $tfr;
}

$tfr_output .= "</TFRList>\n";

if ($seq > 0) {
    open(OUTPUT, ">$TFR_OUTPUT_FILE") or die;
    print OUTPUT $tfr_output;
}

print "Loaded $seq TFRs\n";