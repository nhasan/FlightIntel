/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.utils;

import java.io.File;

public class FileUtils {

    public static void removeDir( File dir ) {
        File[] files = dir.listFiles();
        if ( files != null ) {
            for ( File file : files ) {
                if ( file.isDirectory() ) {
                    removeDir( file );
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

}
