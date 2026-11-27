#!/bin/bash

#/*
# * FlightIntel for Pilots
# *
# * Copyright 2019-2026 Nadeem Hasan <nhasan@nadmm.com>
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

# Builds the FlightIntel data files for the cycle in build_data.conf and
# syncs them to Google Storage.
#
# The data sets (FADDS, d-TPP, d-CS, DOF, library) are independent, so they
# are built in parallel. Each one logs to data/logs/<cycle>/<name>.log. A data
# set whose .db.gz already exists is skipped. If any build fails, nothing is
# synced.
#
# Ubuntu package dependencies:
#   sudo apt install curl unzip libdbi-perl libdbd-sqlite3-perl libtext-autoformat-perl \
#       libtext-csv-perl libjson-perl
#   sudo apt install pigz      # optional, faster compression
#   sudo apt install python3 pyenv
#
# Install Google Cloud CLI: https://docs.cloud.google.com/sdk/docs/install-sdk#deb

set -uo pipefail

cd "$(dirname "$(readlink -f "$0")")" || exit 1

usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Builds the data files for the cycle in build_data.conf and syncs them to
Google Storage.

  -s, --serial    Build one data set at a time, with full output on the console
  -n, --no-sync   Build only, skip the Google Storage sync
  -h, --help      Show this help
EOF
}

SERIAL=0
SYNC=1
while (( $# )); do
    case $1 in
        -s|--serial)  SERIAL=1 ;;
        -n|--no-sync) SYNC=0 ;;
        -h|--help)    usage; exit 0 ;;
        *)            usage >&2; exit 2 ;;
    esac
    shift
done

# ---------------------------------------------------------------------------
# Console output
# ---------------------------------------------------------------------------

if [[ -t 1 ]]; then
    TTY=1
    C_HEAD=$(tput bold; tput setaf 39)
    C_ERR=$(tput bold; tput setaf 125)
    C_OK=$(tput setaf 2)
    C_DIM=$(tput setaf 245)
    C_OFF=$(tput sgr0)
else
    TTY=0
    C_HEAD='' C_ERR='' C_OK='' C_DIM='' C_OFF=''
fi

heading() { printf '\n%s%s%s\n' "$C_HEAD" "$*" "$C_OFF"; }
error()   { printf '%s%s%s\n' "$C_ERR" "$*" "$C_OFF" >&2; }

# Formats a number of seconds as 1h02m03s, 2m03s or 3s.
fmt_time() {
    local s=$1
    if (( s >= 3600 )); then
        printf '%dh%02dm%02ds' $((s / 3600)) $((s % 3600 / 60)) $((s % 60))
    elif (( s >= 60 )); then
        printf '%dm%02ds' $((s / 60)) $((s % 60))
    else
        printf '%ds' "$s"
    fi
}

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

source ./build_data.conf
for var in DATA_CYCLE FADDS_CYCLE DTPP_CYCLE DCS_CYCLE; do
    if [[ -z ${!var:-} ]]; then
        error "build_data.conf: ${var} is not set"
        exit 1
    fi
done

DATA_DIR=data
OUT_DIR=${DATA_DIR}/database
LIBRARY_DIR=${DATA_DIR}/library
LIBRARY_CONFIG=library_data.csv
LOG_DIR=${DATA_DIR}/logs/${DATA_CYCLE}
GS_DATA_URL=gs://flightintel/database
GS_LIBRARY_URL=gs://flightintel/library

if command -v pigz > /dev/null; then
    GZIP_CMD=pigz
else
    GZIP_CMD=gzip
fi

mkdir -p "${OUT_DIR}" "${LIBRARY_DIR}" "${LOG_DIR}"

heading "FlightIntel data build"
printf '  %-12s %s\n' \
    "DATA_CYCLE"  "${DATA_CYCLE}" \
    "FADDS_CYCLE" "${FADDS_CYCLE}" \
    "DTPP_CYCLE"  "${DTPP_CYCLE}" \
    "DCS_CYCLE"   "${DCS_CYCLE}"
printf '  %s%-12s %s%s\n' "${C_DIM}" "Logs" "${LOG_DIR}/" "${C_OFF}"

# ---------------------------------------------------------------------------
# Job helpers. These run inside a job subshell with `set -e`, so any failing
# command fails the job. $JOB is the name of the running job.
# ---------------------------------------------------------------------------

# step LABEL [DETAIL]: shows LABEL on the status line and logs a step header.
step() {
    printf '%s\n' "$1" > "${LOG_DIR}/${JOB}.step"
    printf '\n==> [%(%T)T] %s\n' -1 "${2:-$1}"
}

# run CMD [ARGS...]: runs a build step.
run() {
    step "$(basename "$1")" "$*"
    "$@"
}

# run_optional CMD [ARGS...]: runs a step whose failure only warns.
run_optional() {
    step "$(basename "$1")" "$*"
    if ! "$@"; then
        echo "WARNING: $(basename "$1") failed, continuing"
    fi
}

# download URL DEST: downloads URL unless DEST already exists. Downloads go to
# a .part file first, so a failed download is never mistaken for a good one.
download() {
    local url=$1 dest=$2
    if [[ -s ${dest} ]]; then
        echo "Using downloaded ${dest}"
        return 0
    fi
    step "downloading $(basename "${dest}")" "Downloading ${url}"
    mkdir -p "$(dirname "${dest}")"
    local progress=(--silent --show-error)
    (( SERIAL && TTY )) && progress=(--progress-bar)
    curl --fail --location --retry 3 --retry-delay 10 "${progress[@]}" \
        --output "${dest}.part" "${url}"
    mv -f -- "${dest}.part" "${dest}"
}

# extract ZIP DIR: unzips ZIP into DIR unless that already completed.
extract() {
    local zip=$1 dir=$2
    local marker
    marker="${dir}/.extracted-$(basename "${zip}")"
    [[ -f ${marker} ]] && return 0
    step "extracting $(basename "${zip}")" "Extracting ${zip}"
    unzip -o -q -d "${dir}" "${zip}"
    touch "${marker}"
}

# begin_db DB: starts a fresh database. Until end_db, a failed or interrupted
# job deletes DB so a partial database is never compressed or synced.
begin_db() {
    DB=$1
    rm -f -- "${DB}" "${DB}-journal"
    trap 'rm -f -- "${DB}" "${DB}-journal"' EXIT
}

# end_db: compresses the finished database.
end_db() {
    step "compressing" "Compressing ${DB}"
    "${GZIP_CMD}" -f -- "${DB}"
    trap - EXIT
    ls -l "${DB}.gz"
}

out_file() { printf '%s/%s_%s.db' "${OUT_DIR}" "$1" "${DATA_CYCLE}"; }

# ---------------------------------------------------------------------------
# Jobs
# ---------------------------------------------------------------------------

job_fadds() {
    local src=${DATA_DIR}/FADDS/${DATA_CYCLE}
    local prev=${OUT_DIR}/fadds_prev.db
    local name=28DaySubscription_Effective_${FADDS_CYCLE}.zip
    download "https://nfdc.faa.gov/webContent/28DaySub/${name}" "${src}/${name}"
    extract "${src}/${name}" "${src}"
    begin_db "$(out_file fadds)"
    run ./load_fadds.pl "${src}" "${DB}"
    run ./load_wx_stations.pl "${src}" "${DB}"
    run ./load_atc_phones.pl "${DB}"
    run ./cached_tz_lookup.pl "${DB}" "${prev}"
    # Online lookup for airports with no cached timezone. Best effort: an API
    # outage should not fail the whole build.
    run_optional ./geonames_tz_lookup.pl "${DB}"
    step "saving fadds_prev.db" "Copying ${DB} to ${prev}"
    cp -f -- "${DB}" "${prev}"
    end_db
}

job_dtpp() {
    local src=${DATA_DIR}/d-TPP/${DTPP_CYCLE}
    download "https://aeronav.faa.gov/d-tpp/${DTPP_CYCLE}/xml_data/d-TPP_Metafile.xml" \
        "${src}/d-TPP_Metafile.xml"
    begin_db "$(out_file dtpp)"
    run ./load_dtpp_metadata.pl "${src}" "${DB}"
    end_db
}

job_dcs() {
    local src=${DATA_DIR}/d-CS/${DCS_CYCLE}
    local name=afd_${DCS_CYCLE}.xml
    download "https://aeronav.faa.gov/afd/${DCS_CYCLE}/${name}" "${src}/${name}"
    begin_db "$(out_file dcs)"
    run ./load_dafd_metadata.pl "${DCS_CYCLE}" "${src}" "${DB}"
    end_db
}

job_dof() {
    local src=${DATA_DIR}/DOF/${DATA_CYCLE}
    local name=DAILY_DOF_DAT.ZIP
    download "https://aeronav.faa.gov/Obst_Data/${name}" "${src}/${name}"
    extract "${src}/${name}" "${src}"
    begin_db "$(out_file dof)"
    run ./load_dof.pl "${src}" "${DB}"
    end_db
}

job_library() {
    local -a pdfs
    mapfile -d '' pdfs < <(find "${LIBRARY_DIR}" -name '*.pdf' -print0)
    if (( ${#pdfs[@]} )); then
        step "compressing ${#pdfs[@]} PDFs" "Compressing ${#pdfs[@]} library PDFs"
        printf '%s\0' "${pdfs[@]}" | xargs -0 -n 1 -P "$(nproc)" "${GZIP_CMD}" -f -v --
    fi
    # load_library.pl records the size of each .pdf.gz, so it runs after
    # the PDFs are compressed.
    begin_db "$(out_file library)"
    run ./load_library.pl "${LIBRARY_DIR}" "${DB}"
    end_db
}

# gcloud matches --exclude with re.match, which anchors only at the start, so
# each pattern needs a trailing $ (".*\.pdf" would also exclude "x.pdf.gz").
job_sync_data() {
    run gcloud storage rsync "${OUT_DIR}" "${GS_DATA_URL}" --recursive \
        --delete-unmatched-destination-objects \
        --exclude '.*\.(db|db-journal|part)$' \
        --predefined-acl publicRead
}

job_sync_library() {
    run gcloud storage rsync "${LIBRARY_DIR}" "${GS_LIBRARY_URL}" --recursive \
        --delete-unmatched-destination-objects \
        --exclude '.*\.(pdf|part)$' \
        --predefined-acl publicRead
}

# Runs job NAME in the current (sub)shell.
job_main() {
    set -e -o pipefail
    trap 'exit 130' INT TERM
    JOB=$1
    printf '%s: started %(%F %T)T\n' "${JOB}" -1
    "job_$1"
    printf '\n%s: finished %(%F %T)T\n' "${JOB}" -1
}

# ---------------------------------------------------------------------------
# Job runner
# ---------------------------------------------------------------------------

declare -A PID START
RUNNING=()
FAILED=()

# A file descriptor that never has input, so `read -t` can sleep without
# starting an external process.
exec {SLEEP_FD}<> <(:)
nap() { read -r -t "$1" -u "${SLEEP_FD}" || true; }

# read_step NAME: prints the current step of job NAME.
read_step() { cat -- "${LOG_DIR}/$1.step" 2> /dev/null || echo "starting"; }

# start_step NAME: resets the step of job NAME before it starts, so the status
# line never reads a missing or stale step file.
start_step() { echo "starting" > "${LOG_DIR}/$1.step"; }

clear_status() { if (( TTY && !SERIAL )); then printf '\r\e[K'; fi; }

# report NAME RC: prints the result of a finished job.
report() {
    local name=$1 rc=$2 log=${LOG_DIR}/$1.log
    local elapsed
    elapsed=$(fmt_time $((SECONDS - START[$1])))
    clear_status
    if (( rc == 0 )); then
        printf '  %s✔%s %-14s %-44s %s%s%s\n' "${C_OK}" "${C_OFF}" "${name}" \
            "done" "${C_DIM}" "${elapsed}" "${C_OFF}"
    else
        FAILED+=("${name}")
        local where
        where=$(read_step "${name}")
        printf '  %s✘ %-14s %-44s %s%s\n' "${C_ERR}" "${name}" \
            "failed: ${where}" "${elapsed}" "${C_OFF}"
        if (( !SERIAL )); then
            printf '%s' "${C_DIM}"
            tr '\r' '\n' < "${log}" | grep -v '^[[:space:]]*$' | tail -n 15 | sed 's/^/      │ /'
            printf '      └ full log: %s%s\n' "${log}" "${C_OFF}"
        fi
    fi
}

# skip NAME REASON: reports a job that does not need to run.
skip() {
    printf '  %s• %-14s %s%s\n' "${C_DIM}" "$1" "$2" "${C_OFF}"
}

# Draws the status line: a spinner, then each running job's current step.
draw_status() {
    local frames='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏' name line='' sep=''
    for name in "${RUNNING[@]}"; do
        line+="${sep}${name}: $(read_step "${name}") $(fmt_time $((SECONDS - START[${name}])))"
        sep='  │  '
    done
    line="${frames:$((TICK % 10)):1} ${line}"
    printf '\r\e[K%s%s%s' "${C_DIM}" "${line:0:$((COLS - 1))}" "${C_OFF}"
}

on_interrupt() {
    clear_status
    error "Interrupted, stopping jobs..."
    local name
    for name in "${RUNNING[@]}"; do
        kill -TERM -- "-${PID[${name}]}" 2> /dev/null
    done
    wait
    exit 130
}

# run_jobs NAME...: runs the jobs, in parallel unless --serial was given.
run_jobs() {
    local name rc
    if (( SERIAL )); then
        for name in "$@"; do
            printf '\n%s── %s %s%s\n' "${C_HEAD}" "${name}" \
                "──────────────────────────────────────────" "${C_OFF}"
            START[${name}]=${SECONDS}
            start_step "${name}"
            ( job_main "${name}" ) < /dev/null 2>&1 | tee "${LOG_DIR}/${name}.log"
            rc=${PIPESTATUS[0]}
            (( rc == 130 )) && exit 130
            report "${name}" "${rc}"
        done
        return 0
    fi

    # Job control puts each job in its own process group, so an interrupt can
    # stop a job together with the loader it is running.
    set -m
    trap on_interrupt INT TERM
    for name in "$@"; do
        START[${name}]=${SECONDS}
        start_step "${name}"
        ( job_main "${name}" ) < /dev/null > "${LOG_DIR}/${name}.log" 2>&1 &
        PID[${name}]=$!
        RUNNING+=("${name}")
        (( TTY )) || printf '  - %-14s started\n' "${name}"
    done

    TICK=0
    while (( ${#RUNNING[@]} )); do
        local still=()
        for name in "${RUNNING[@]}"; do
            if kill -0 "${PID[${name}]}" 2> /dev/null; then
                still+=("${name}")
            else
                wait "${PID[${name}]}"
                rc=$?
                report "${name}" "${rc}"
            fi
        done
        RUNNING=("${still[@]}")
        if (( ${#RUNNING[@]} )); then
            (( TTY )) && draw_status
            nap 0.25
            (( ++TICK ))
        fi
    done
    trap - INT TERM
    set +m
    return 0
}

COLS=$(tput cols 2> /dev/null || echo 80)
trap 'COLS=$(tput cols 2> /dev/null || echo 80)' WINCH

# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------

heading "Building data files"

BUILD=()
for name in fadds dtpp dcs dof; do
    if [[ -f $(out_file "${name}").gz ]]; then
        skip "${name}" "up to date: $(out_file "${name}").gz"
    else
        BUILD+=("${name}")
    fi
done

# The library database lists each PDF with its compressed size, so rebuild it
# when the config changed or there are new PDFs to compress.
library_gz=$(out_file library).gz
if [[ ! -f ${library_gz} || ${LIBRARY_CONFIG} -nt ${library_gz} ]] \
        || [[ -n $(find "${LIBRARY_DIR}" -name '*.pdf' -print -quit) ]]; then
    BUILD+=(library)
else
    skip library "up to date: ${library_gz}"
fi

BUILD_START=${SECONDS}
if (( ${#BUILD[@]} )); then
    run_jobs "${BUILD[@]}"
fi

if (( ${#FAILED[@]} )); then
    heading "Build failed"
    error "Failed: ${FAILED[*]}. Nothing was synced to Google Storage."
    exit 1
fi

# ---------------------------------------------------------------------------
# Sync
# ---------------------------------------------------------------------------

if (( SYNC )); then
    heading "Syncing with Google Storage"
    run_jobs sync_data sync_library
    if (( ${#FAILED[@]} )); then
        heading "Sync failed"
        error "Failed: ${FAILED[*]}"
        exit 1
    fi
else
    heading "Skipping Google Storage sync (--no-sync)"
fi

heading "Done in $(fmt_time $((SECONDS - BUILD_START)))"
