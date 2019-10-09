#!/usr/bin/env python3

import urllib3
import asyncio
import websockets
import json
from datetime import datetime
import time
import re
import os
from influxdb import InfluxDBClient
import colorama
import atexit

urllib3.disable_warnings()

justnow = int(time.time())-3*60
lastrun = 1570295921
starting_ts = justnow

WSS = os.environ.get("LDP_WSS") + "&begin={tsbegin}".format(tsbegin=starting_ts)
WRITE_TOKEN = os.environ.get("METRICS_WRITE_TOKEN")


def debug(msg, *args, **kwargs):
    print(colorama.Fore.RED + str(msg) + colorama.Style.RESET_ALL, *args, **kwargs)


def reg_finished_task_duration(ts, host, line, result):
    points = []
    if int(result.group('task').split('.')[0])+1 == int(result.group('total')):
        points.append({
            "measurement": "stage",
            "tags": {
                "id": result.group('stage')
            },
            "time": ts.isoformat(),
            "fields": {
                "status": "finished"
            }
        })
    points.append({
        "measurement": "task",
        "tags": {
            "stage": result.group('stage')
            # "host": host,
            # "executor": result.group('executor')
        },
        "time": ts.isoformat(),
        "fields": {
            "duration_ms": result.group('ms'),
            "tid": result.group('TID')
        }
    })
    return points


def reg_started_task(ts, host, line, result):
    points = []
    if result.group('task') == '0.0':
        points.append({
            "measurement": "stage",
            "tags": {
                "id": result.group('stage')
            },
            "time": ts.isoformat(),
            "fields": {
                "status": "started"
            }
        })
    return points


def reg_file_split(ts, host, line, result):
    points = []
    points.append({
        "measurement": "file_split",
        "tags": {
#            "host": host,
            "file": result.group('file')
        },
        "time": ts.isoformat(),
        "fields": {
            "offset": result.group('offset'),
            "len": result.group('len')
        }
    })
    return points


def process(metrics, ts, host, line):
    regs = [
        (re.compile(r"Starting task (?P<task>[\d\.]+) in stage (?P<stage>[\d\.]+) \(TID (?P<TID>\d+), .*\)"), reg_started_task),
        (re.compile(r"Running task (?P<task>[\d\.]+) in stage (?P<stage>[\d\.]+) \(TID (?P<TID>\d+)\)"), None),
        (re.compile(r"Finished task (?P<task>[\d\.]+) in stage (?P<stage>[\d\.]+) \(TID (?P<TID>\d+)\) in (?P<ms>\d+) ms on .* \(executor (?P<executor>\w+)\) \(\d+/(?P<total>\d+)\)"), reg_finished_task_duration),
        (re.compile(r"Finished task (?P<task>[\d\.]+) in stage (?P<stage>[\d\.]+) \(TID (?P<TID>\d+)\). (?P<bytes>\d+) bytes result sent to driver"), None),
        (re.compile(r"Input split: (?P<file>.*):(?P<offset>\d+)\+(?P<len>\d+)"), reg_file_split)
    ]
    for reg, process in regs:
        if callable(process):
            result = reg.match(line)
            if result:
                points = process(ts, host, line, result)
                if points:
                    debug(points)
                    metrics.write_points(points)


async def hello(metrics):
    uri = WSS
    async with websockets.connect(uri) as websocket:
        while True:
            evt = await websocket.recv()
            message = json.loads(json.loads(evt)['message'])
            # ts1 = datetime.strptime(message['_Time'], '%Y-%m-%d %H:%M:%S,%f')
            ts = datetime.fromtimestamp(message['timestamp'])
            print("{ts} {msg[host]} {msg[full_message]}".format(ts=ts, msg=message))
            process(metrics, ts, message['host'], message['full_message'])


if __name__ == '__main__':
    atexit.register(colorama.deinit)
    colorama.init()
    debug(WSS)
    client = InfluxDBClient(
        host='influxdb.gra1.metrics.ovh.net',
        port=443,
        username='metrics',
        password=WRITE_TOKEN,
        ssl=True
    )
    asyncio.get_event_loop().run_until_complete(hello(client))
