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
use XML::LibXML;
use v5.10;

my $TFR_URL_BASE = "https://tfr.faa.gov/tfr2/";
my $TFR_URL = "$TFR_URL_BASE/list.html";
my $FEET_PER_METER = 3.28084;
my $TFR_OUTPUT_FILE = "/var/www/api.flightintel.com/html/data/tfr_list.xml";

my %links = ();

# Order of strings is important
my @tfr_types = ("Special", "Security", "Space Operations", "VIP", "Hazards");

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

# LibXML
my $doc;
my $root;
my $tfr_elem;
my $node;

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
        $tfr_elem = $doc->createElement("TFR");
        $root->appendChild($tfr_elem);
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
        $node = $doc->createElement("NID");
        $node->appendTextNode("${dateIndexYear}/${noSeqNo}");
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "dateEffective" && !$TFRAreaGroup) {
        $node = $doc->createElement("ACTIVE");
        $node->appendTextNode("${text}Z");
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "dateExpire" && !$TFRAreaGroup) {
        $node = $doc->createElement("EXPIRES");
        $node->appendTextNode("${text}Z");
        $tfr_elem->appendChild($node); 
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
        $node = $doc->createElement("CREATED");
        $node->appendTextNode("${text}Z");
        $tfr_elem->appendChild($node); 
        $node = $doc->createElement("MODIFIED");
        $node->appendTextNode("${text}Z");
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "dateIndexYear") {
        $dateIndexYear = $text % 10;
    }
    elsif ($elem eq "noSeqNo") {
        $noSeqNo = $text;
    }
    elsif ($elem eq "txtLocalName") {
        $node = $doc->createElement("NAME");
        $node->appendTextNode($text);
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "txtDescrUSNS") {
        $node = $doc->createElement("SRC");
        $node->appendTextNode($text);
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "txtDescrModern" ) {
        foreach my $tfr_type (@tfr_types) {
            if ($text =~ /\>$tfr_type\</) {
                $node = $doc->createElement("TYPE");
                $node->appendTextNode($tfr_type);
                $tfr_elem->appendChild($node); 
                last;
            }
        }
    }
    elsif ($elem eq "codeFacility") {
        $node = $doc->createElement("FACILITY");
        $node->appendTextNode($text);
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "txtNameCity") {
        $node = $doc->createElement("CITY");
        $node->appendTextNode(capitalize($text));
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "txtNameUSState") {
        $node = $doc->createElement("STATE");
        $node->appendTextNode(capitalize($text));
        $tfr_elem->appendChild($node); 
    }
    elsif ($elem eq "codeCoordFacilityType") {
        $node = $doc->createElement("FACILITYTYPE");
        $node->appendTextNode($text);
        $tfr_elem->appendChild($node); 
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

        $node = $doc->createElement("MINALT");
        $node->appendTextNode("${valLower}${codeLower}");
        $tfr_elem->appendChild($node); 
        $node = $doc->createElement("MAXALT");
        $node->appendTextNode("${valUpper}${codeUpper}");
        $tfr_elem->appendChild($node); 
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
my $now = strftime "%FT%TZ", gmtime time;
$doc = XML::LibXML::Document->new("1.0", "UTF-8");
$root = $doc->createElement("TFRList");

foreach my $url (keys %links) {
    if (my $xml = get($url) ) {
        print "Fetched $url\n";
        $seq++;
        $parser->parse($xml);
    } else {
        print "Failed  $url\n";
    }
}

$root->setAttribute("count", $seq);
$root->setAttribute("timestamp", $now);
$doc->setDocumentElement($root);

if ($seq > 0) {
    open(OUTPUT, ">$TFR_OUTPUT_FILE") or die;
    print OUTPUT $doc->toString(1);
}

print "Loaded $seq TFRs\n";
