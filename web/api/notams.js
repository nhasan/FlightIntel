/*
 * FlightIntel
 *
 * Copyright 2021 Nadeem Hasan <nhasan@nadmm.com>
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

const sqlite3 = require('sqlite3').verbose();
const config = require('config');

let db = new sqlite3.Database(config.get("Notams.dbname"), 
    sqlite3.OPEN_READONLY,
    (err) => { 
        if (err) {
            return console.error(err.message);
        }
    }
);

let getNotams = function (location, finish) {
    db.all('SELECT * FROM notams WHERE location = ? ORDER BY lastUpdated DESC',
    [location], 
    (err, rows) => {
        if (err) {
            finish({ error: err.message });
        } else {
            finish(rows);
        }
    });
}

const express = require('express');
const router = express.Router();

router.get('/:location', function (req, res) {
    getNotams(req.params.location,
        function finish(result) {
            if (result.hasOwnProperty("error")) {
                res.status(500);
            }
            res.json(result)
        });
});

module.exports = router;
