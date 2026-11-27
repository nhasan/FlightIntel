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

# Builds tfr_list.xml for the app from the FAA TFR site.
#
# Usage: load_tfr.pl [output_dir]
#
# Writes tfr_list.xml and tfr_control.txt to output_dir, which defaults to the
# web root of the swag container on api.flightintel.com.
#
# The TFR list comes from the tfr.faa.gov JSON API, and each TFR's details from
# its XNOTAM XML file on the same site.
#
# Ubuntu package dependencies
#    liblwp-protocol-https-perl
#    libtext-autoformat-perl
#    libxml-parser-perl
#    libxml-libxml-perl

use strict;
use warnings;
use POSIX;
use JSON::PP;
use LWP::UserAgent;
use Text::Autoformat;
use XML::Parser;
use XML::LibXML;
use v5.10;

my $TFR_LIST_URL = "https://tfr.faa.gov/tfrapi/getTfrList";
my $TFR_DETAIL_URL = "https://tfr.faa.gov/download/detail_%s.xml";
my $FEET_PER_METER = 3.28084;
my $TFR_OUTPUT_DIR = shift @ARGV // "$ENV{HOME}/sw/containers/swag/www/data";
my $TFR_OUTPUT_FILE = "$TFR_OUTPUT_DIR/tfr_list.xml";
my $TFR_CONTROL_FILE = "$TFR_OUTPUT_DIR/tfr_control.txt";

# TFR type from the list API, used in place of parsing txtDescrModern
my $list_type;

# Order of strings is important
my @tfr_types = ("Special", "Security", "Space Operations", "VIP", "Hazards", "Airshow");

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

# Maps a list API type such as "SPACE OPERATIONS" to the names the app has
# always shown, such as "Space Operations".
sub tfr_type($)
{
    my ( $type ) = @_;
    return undef if !defined $type or $type eq "";
    return "VIP" if $type eq "VIP";
    return "Airshow" if $type =~ /^AIR SHOW/;
    return join( " ", map { ucfirst lc } split( / /, $type ) );
}

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
        if (defined $list_type) {
            $node = $doc->createElement("TYPE");
            $node->appendTextNode($list_type);
            $tfr_elem->appendChild($node);
            return;
        }
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
                # Whole feet: the app parses altitudes as integers
                $valDistVerLower[$index] = int($valDistVerLower[$index] * $FEET_PER_METER + 0.5);
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
                $valDistVerUpper[$index] = int($valDistVerUpper[$index] * $FEET_PER_METER + 0.5);
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

my $ua = LWP::UserAgent->new( timeout => 30 );

my $response = $ua->get($TFR_LIST_URL);
die "Unable to fetch $TFR_LIST_URL: ".$response->status_line."\n"
    unless $response->is_success;
my $list = JSON::PP->new->decode($response->decoded_content);

my $seq = 0;
my $failed = 0;
my %seen = ();
my $now = strftime "%FT%TZ", gmtime time;
$doc = XML::LibXML::Document->new("1.0", "UTF-8");
$root = $doc->createElement("TFRList");

foreach my $entry (@$list) {
    my $notam_id = $entry->{notam_id} or next;
    next if $seen{$notam_id}++;

    (my $file_id = $notam_id) =~ s{/}{_}g;
    my $url = sprintf($TFR_DETAIL_URL, $file_id);
    $response = $ua->get($url);
    if (!$response->is_success) {
        print "Failed  $url: ".$response->status_line."\n";
        $failed++;
        next;
    }

    $list_type = tfr_type($entry->{type});
    # A malformed file must not stop the rest of the list from loading
    if (eval { $parser->parse($response->content); 1 }) {
        print "Fetched $url\n";
        $seq++;
    } else {
        print "Failed  $url: $@";
        $failed++;
        # Drop the partial TFR element the parser may have started
        $root->removeChild($tfr_elem)
            if defined $tfr_elem and $tfr_elem->parentNode
                and $tfr_elem->parentNode->isSameNode($root);
    }
    undef $tfr_elem;
}

$root->setAttribute("count", $seq);
$root->setAttribute("timestamp", $now);
$doc->setDocumentElement($root);

if ($seq > 0) {
    # Write to a temporary file and rename it, so the app never downloads a
    # partly written list.
    write_file($TFR_OUTPUT_FILE, $doc->toString(1));
    write_file($TFR_CONTROL_FILE, "Loaded $seq TFRs");
}

sub write_file
{
    my ( $file, $content ) = @_;
    my $tmp_file = "$file.tmp";
    open(my $out, ">", $tmp_file) or die "Unable to write $tmp_file: $!\n";
    print $out $content;
    close($out) or die "Unable to write $tmp_file: $!\n";
    rename($tmp_file, $file) or die "Unable to rename $tmp_file: $!\n";
}

print "Loaded $seq TFRs, $failed failed\n";
exit($seq > 0 ? 0 : 1);
