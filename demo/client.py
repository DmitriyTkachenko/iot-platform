import json
import requests
import time
import random

domain = 'http://localhost:8080'
login = 'test_client'
password = 'password'
r = requests.get('{0}/api/auth/login/{1}/password/{2}'.format(domain, login, password))
token = r.json()['token']
auth_header = {'Authorization': 'Bearer {0}'.format(token)}


def send_data():
    data = {'data': {'temperature': random.normalvariate(25, 1)}}
    print('Sending data')
    requests.post('{0}/api/data'.format(domain), data=json.dumps(data), headers=auth_header)


def send_data_continuously(interval):
    while True:
        send_data()
        time.sleep(interval)


send_interval_seconds = 0.1
send_data_continuously(send_interval_seconds)
