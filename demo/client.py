import datetime
import json
import os
import time

import requests

domain = 'http://localhost:8080'
login = 'RaspberryPi'
password = 'pass'
r = requests.get('{0}/api/auth/login/{1}/password/{2}'.format(domain, login, password))
token = r.json()['token']
auth_header = {'Authorization': 'Bearer {0}'.format(token)}


def send_data():
    temperature_data = get_cpu_temperature()
    data = {'data': {'temperature': temperature_data}}
    print('{0}. Sending data: {1}'.format(datetime.datetime.utcnow(), str(temperature_data)))
    requests.post('{0}/api/data'.format(domain), data=json.dumps(data), headers=auth_header)


def send_data_continuously(interval):
    while True:
        send_data()
        time.sleep(interval)


def get_cpu_temperature():
    res = os.popen('vcgencmd measure_temp').readline()
    return float(res.replace("temp=", "").replace("'C\n", ""))


send_interval_seconds = 0.025
send_data_continuously(send_interval_seconds)
