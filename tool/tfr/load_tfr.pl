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
#    libhtml-parser-perl

use strict;
use warnings;
use POSIX;
use JSON::PP;
use HTML::Entities;
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

# Shape of each TFR area. The FAA publishes it in abdMergedArea as a closed
# polygon with circles and arcs already converted to points.
my @elem_path;
my @shape_points;
my $avx_lat;
my $avx_lon;
# Vertical limits of the current area group, by element name
my %area_alt;
# Effective times of the current area group from its first ScheduleGroup
my %area_time;
my $area_schedules;
# Definition of the current area group: its name, and the shapes it is built
# from (aseShapes), each a list of vertices, with how each one combines with
# the others (BASE, UNION or SUBTR) by shape id
my $area_name;
my @area_parts;
my %part;
my %vertex;
my %part_op;
my %aac;
# Details of the current TFR written once its Not element ends
my $replaced_id;
my $replaced_year;
my $replaced_seq;
my $replaced_reason;
my $purpose;
my $html;

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

# Converts an AIXM coordinate such as "080.70527778W" to signed decimal degrees
# rounded to 5 places (about 1 m).
sub parse_coord($)
{
    my ( $coord ) = @_;
    return undef unless defined $coord and $coord =~ /^\s*([0-9.]+)\s*([NSEW])\s*$/;
    my $value = $1;
    $value = -$value if $2 eq "S" or $2 eq "W";
    return sprintf( "%.5f", $value ) + 0;
}

# True when the element being parsed is a direct child of a vertex (Avx) of a
# merged area. Vertices also hold geoLat/geoLong of reference navaids in Frd,
# which must be skipped.
sub in_merged_vertex()
{
    return @elem_path >= 3 && $elem_path[-2] eq "Avx" && $elem_path[-3] eq "abdMergedArea";
}

# Formats a vertical limit like MINALT/MAXALT: whole feet followed by A (AGL)
# or M (MSL). Flight levels are pressure altitudes, so they count as MSL.
sub area_alt($)
{
    my ( $which ) = @_;
    my $val = $area_alt{"valDistVer$which"};
    my $uom = $area_alt{"uomDistVer$which"} // "FT";
    my $code = $area_alt{"codeDistVer$which"} // "ALT";
    return undef unless defined $val and $val =~ /^\s*(\d+(?:\.\d+)?)\s*$/;
    $val = $1;
    if ($uom eq "M") {
        $val *= $FEET_PER_METER;
    }
    elsif ($uom eq "FL") {
        $val *= 100;
        $code = "ALT";
    }
    return int($val + 0.5) . ($code eq "HEI" ? "A" : "M");
}

# True when the parents of the element that just ended end with the given names
sub parents_end
{
    my @names = @_;
    return 0 if @elem_path < @names;
    for my $i (1 .. @names) {
        return 0 if $elem_path[-$i] ne $names[-$i];
    }
    return 1;
}

# The vertical limits and effective times of the current area group as
# attributes of an element
sub set_area_attrs($)
{
    my ( $node ) = @_;
    my $min = area_alt("Lower");
    my $max = area_alt("Upper");
    $node->setAttribute("minalt", $min) if defined $min;
    $node->setAttribute("maxalt", $max) if defined $max;
    # With isTimeSeparate, startTime and endTime give the daily window on the
    # first day
    my %attrs = (active => "dateEffective", expires => "dateExpire");
    if (($area_time{isTimeSeparate} // "") eq "TRUE") {
        $attrs{dailystart} = "startTime";
        $attrs{dailyend} = "endTime";
    }
    foreach my $attr (sort keys %attrs) {
        my $value = $area_time{$attrs{$attr}};
        $node->setAttribute($attr, "${value}Z")
            if defined $value and $value =~ /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d$/;
    }
}

# Converts a distance to nautical miles, rounded to 0.01
sub to_nm($$)
{
    my ( $value, $uom ) = @_;
    return undef unless defined $value and $value =~ /^\s*(\d+(?:\.\d+)?)\s*$/;
    my %per_nm = (NM => 1, KM => 1.852, M => 1852, FT => 6076.12, MI => 1.15078);
    my $factor = $per_nm{uc($uom // "NM")} or return undef;
    return sprintf("%.2f", $1 / $factor) + 0;
}

# Writes one vertex of an area shape: a CIRCLE, a POINT of a polygon, or the
# center of an ARC that runs to the next point. Each may give its position as
# a fix-radial-distance from a navaid.
sub vertex_node(\%)
{
    my ( $v ) = @_;
    my $type = $v->{codeType} // "";
    my $node;
    my ($lat, $lon);
    if ($type eq "CIR") {
        $node = $doc->createElement("CIRCLE");
        $lat = parse_coord($v->{geoLat} // $v->{geoLatArc});
        $lon = parse_coord($v->{geoLong} // $v->{geoLongArc});
    }
    elsif ($type eq "CWA" or $type eq "CCA") {
        $node = $doc->createElement("ARC");
        $node->setAttribute("dir", $type eq "CWA" ? "CW" : "CCW");
        $lat = parse_coord($v->{geoLatArc} // $v->{geoLat});
        $lon = parse_coord($v->{geoLongArc} // $v->{geoLong});
    }
    else {
        $node = $doc->createElement("POINT");
        $lat = parse_coord($v->{geoLat});
        $lon = parse_coord($v->{geoLong});
    }
    return undef unless defined $lat and defined $lon;
    $node->setAttribute("lat", $lat);
    $node->setAttribute("lon", $lon);
    if ($type ne "GRC") {
        my $radius = to_nm($v->{valRadiusArc}, $v->{uomRadiusArc});
        $node->setAttribute("radius", $radius) if defined $radius;
    }
    my $distance = to_nm($v->{valDist}, $v->{uomDist});
    if (defined $v->{ident} and defined $v->{bearing} and defined $distance
            and $v->{bearing} =~ /^\s*(\d+(?:\.\d+)?)\s*$/) {
        $node->setAttribute("navaid", capitalize($v->{navaid})) if $v->{navaid};
        my $ain = $v->{ain} // "";
        my $din = $v->{din} // "";
        my $navtype = $ain eq "VorUid" && $din eq "TcnUid" ? "VORTAC"
            : $ain eq "TcnUid" ? "TACAN"
            : $ain eq "VorUid" && $din eq "DmeUid" ? "VOR/DME"
            : $ain eq "VorUid" ? "VOR"
            : $ain eq "NdbUid" && $din eq "DmeUid" ? "NDB/DME"
            : $ain eq "NdbUid" ? "NDB"
            : undef;
        $node->setAttribute("navtype", $navtype) if defined $navtype;
        $node->setAttribute("ident", $v->{ident});
        $node->setAttribute("bearing", int($1 + 0.5) % 360);
        $node->setAttribute("distance", $distance);
    }
    return $node;
}

# Writes the current area group as an AREA element: its name, vertical limits,
# effective times and the shapes that define it
sub area_node()
{
    my $node = $doc->createElement("AREA");
    $node->setAttribute("name", $area_name) if defined $area_name and $area_name ne "";
    set_area_attrs($node);
    # The base shape first, then those added to or taken from it
    my %rank = (BASE => 0, UNION => 1, SUBTR => 2);
    my @parts = sort {
        ($rank{$part_op{$a->{id} // ""} // "BASE"} // 1)
            <=> ($rank{$part_op{$b->{id} // ""} // "BASE"} // 1)
    } @area_parts;
    foreach my $p (@parts) {
        my $part_node = $doc->createElement("PART");
        my $op = $part_op{$p->{id} // ""} // "BASE";
        $part_node->setAttribute("op", $op);
        foreach my $v (@{$p->{vertices}}) {
            my $vertex_node = vertex_node(%$v);
            $part_node->appendChild($vertex_node) if defined $vertex_node;
        }
        $node->appendChild($part_node) if $part_node->hasChildNodes;
    }
    return $node;
}

# The text of a labeled value in the FAA's HTML description, such as
# "Reason for NOTAM : Temporary flight restrictions for VIP Movement"
sub html_field($$)
{
    my ( $label, $source ) = @_;
    return undef unless defined $source and $source =~ m{
        >\s*\Q$label\E\s*:\s*</font>\s*</TD>\s*<TD[^>]*>\s*<font[^>]*>(.*?)</font>
    }isx;
    return html_text($1);
}

sub html_text($)
{
    my ( $text ) = @_;
    $text =~ s/<[^>]+>/ /g;
    $text = decode_entities($text);
    $text =~ s/\s+/ /g;
    $text =~ s/$reTrim//g;
    return $text eq "" ? undef : $text;
}

sub append_text_node($$)
{
    my ( $name, $value ) = @_;
    return unless defined $value and $value ne "";
    $node = $doc->createElement($name);
    $node->appendTextNode($value);
    $tfr_elem->appendChild($node);
    return $node;
}

# Writes the details gathered over the whole Not element of the TFR
sub tfr_details()
{
    append_text_node("REASON", html_field("Reason for NOTAM", $html) // $purpose);
    my $artcc = html_field("ARTCC", $html);
    if (defined $artcc and $artcc =~ /^\w+\s+-\s+(.+)$/) {
        append_text_node("FACILITYNAME", $1);
    }
    append_text_node("AUTHORITY", html_field("Authority", $html));
    if (defined $replaced_id) {
        my $replaced = append_text_node("REPLACED", $replaced_id);
        $replaced->setAttribute("reason", $replaced_reason)
            if defined $replaced_reason and $replaced_reason ne "";
    }
    # The opening line of the operating restrictions, such as "No pilots may
    # operate an aircraft in the areas covered by this NOTAM (except as
    # described)."
    if (defined $html and $html =~ />\s*(No [^<>]*? covered by this NOTAM[^<>]*?)\s*</i) {
        append_text_node("RESTRICTION", html_text($1));
    }
}

sub handle_start() {
    shift;
    my ($elem, %attrs) = @_;
    $text = "";
    push @elem_path, $elem;

    if ($elem eq "abdMergedArea") {
        @shape_points = ();
    }
    elsif ($elem eq "Avx") {
        undef $avx_lat;
        undef $avx_lon;
        %vertex = ();
    }
    elsif ($elem eq "aseShapes") {
        %part = (vertices => []);
    }
    elsif ($elem eq "Aac") {
        %aac = ();
    }
    elsif ($elem eq "Not") {
        $tfr_elem = $doc->createElement("TFR");
        $root->appendChild($tfr_elem);
        undef $replaced_id;
        undef $replaced_year;
        undef $replaced_seq;
        undef $replaced_reason;
        undef $purpose;
        undef $html;
    }
    elsif ($elem eq "TFRAreaGroup") {
        $TFRAreaGroup = 1;
        %area_alt = ();
        %area_time = ();
        $area_schedules = 0;
        undef $area_name;
        @area_parts = ();
        %part_op = ();
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
    my $merged_vertex = in_merged_vertex();
    pop @elem_path;

    if ($TFRAreaGroup and $elem =~ /^(?:val|uom|code)DistVer(?:Lower|Upper)$/) {
        $area_alt{$elem} = $text;
    }
    if ($TFRAreaGroup and @elem_path and $elem_path[-1] eq "ScheduleGroup"
            and $area_schedules == 0) {
        $area_time{$elem} = $text =~ s/$reTrim//gr;
    }
    elsif ($TFRAreaGroup and $elem eq "ScheduleGroup") {
        $area_schedules++;
    }

    # The shapes that define the area, their vertices, and the navaid each
    # vertex is given from
    my $trimmed = $text =~ s/$reTrim//gr;
    if ($TFRAreaGroup and parents_end("TFRAreaGroup", "aseTFRArea") and $elem eq "txtName") {
        $area_name = $trimmed;
    }
    elsif (parents_end("aseShapes", "AseUid") and $elem eq "codeId") {
        $part{id} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx") and $elem ne "Frd") {
        $vertex{$elem} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx", "Frd") and $elem eq "txtRmk") {
        $vertex{navaid} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx", "Frd", "Ain") and $elem eq "valAngleBrg") {
        $vertex{bearing} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx", "Frd", "Ain") and $elem =~ /Uid$/) {
        $vertex{ain} = $elem;
    }
    elsif ($elem eq "codeId" and @elem_path and $elem_path[-1] =~ /Uid$/
            and parents_end("aseShapes", "Abd", "Avx", "Frd", "Ain", $elem_path[-1])) {
        $vertex{ident} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx", "Frd", "Din") and $elem =~ /^(?:valDist|uomDist)$/) {
        $vertex{$elem} = $trimmed;
    }
    elsif (parents_end("aseShapes", "Abd", "Avx", "Frd", "Din") and $elem =~ /Uid$/) {
        $vertex{din} = $elem;
    }
    elsif (parents_end("TFRAreaGroup", "Aac", "AacUid", "AseUidPar") and $elem eq "codeId") {
        $aac{par} = $trimmed;
    }
    elsif (parents_end("TFRAreaGroup", "Aac") and $elem eq "codeOpr") {
        $aac{op} = $trimmed;
    }
    elsif (parents_end("InstructionsGroup") and $elem eq "txtInstr") {
        append_text_node("INSTRUCTION", $trimmed);
    }
    elsif (parents_end("NotUidMod") and $elem eq "dateIndexYear") {
        $replaced_year = $trimmed % 10;
        return;
    }
    elsif (parents_end("NotUidMod") and $elem eq "noSeqNo") {
        $replaced_seq = $trimmed;
        return;
    }
    if ($elem eq "Avx" and parents_end("aseShapes", "Abd")) {
        push @{$part{vertices}}, { %vertex };
    }
    elsif ($elem eq "aseShapes") {
        push @area_parts, { %part };
    }
    elsif ($elem eq "Aac" and defined $aac{par} and defined $aac{op}) {
        $part_op{$aac{par}} = $aac{op};
    }
    elsif ($elem eq "NotUidMod" and defined $replaced_year and defined $replaced_seq) {
        $replaced_id = "$replaced_year/$replaced_seq";
    }
    elsif ($elem eq "txtDescrMod") {
        $replaced_reason = $trimmed;
    }
    elsif ($elem eq "txtDescrPurpose") {
        $purpose = $trimmed;
    }
    elsif ($elem eq "Not") {
        tfr_details();
    }

    if ($merged_vertex and $elem eq "geoLat") {
        $avx_lat = parse_coord($text);
    }
    elsif ($merged_vertex and $elem eq "geoLong") {
        $avx_lon = parse_coord($text);
    }
    elsif ($elem eq "Avx" and @elem_path and $elem_path[-1] eq "abdMergedArea") {
        push @shape_points, "$avx_lat,$avx_lon" if defined $avx_lat and defined $avx_lon;
    }
    elsif ($elem eq "abdMergedArea") {
        # Each SHAPE is one area as "lat,lon lat,lon ..." in decimal degrees,
        # with the area's own vertical limits when known
        if (@shape_points >= 3) {
            $node = $doc->createElement("SHAPE");
            set_area_attrs($node);
            $node->appendTextNode(join(" ", @shape_points));
            $tfr_elem->appendChild($node);
        }
        @shape_points = ();
    }
    elsif ($elem eq "TFRAreaGroup") {
        my $area = area_node();
        $tfr_elem->appendChild($area) if $area->hasChildNodes;
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
        $html = $text;
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
    @elem_path = ();
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
