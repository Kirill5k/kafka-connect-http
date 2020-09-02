from flask import Flask, json, request, jsonify
import time
import sys
import gzip
import random

api = Flask(__name__)
stats = {}

client_id = 'mock-server-client-id'
client_secret = 'mock-server-client-secret'
access_token = 'mock-server-access-token'


@api.route('/token', methods=['POST'])
def grant_access_token():
  if request.content_type != 'application/x-www-form-urlencoded':
    return jsonify({'message': 'unexpected content type'}), 400

  if request.headers.get('Authorization', '') != 'Basic bW9jay1hcGktY2xpZW50LWlkOm1vY2stYXBpLWNsaWVudC1zZWNyZXQ=':
    return jsonify({'message': 'invalid authorization header'}), 401

  form_data = request.form
  if form_data.get('grant_type', '') != 'client_credentials':
    return jsonify({'message': f'unexpect grant type {form_data.get("grant_type", "")}'}), 400

  if form_data.get('client_id', '') != client_id and form_data.get('client_secret', '') != client_secret:
    return jsonify({'message': 'invalid client id or client secret'}), 401

  return jsonify({
    'access_token': access_token,
    'expires_in': 7200,
    'token_type': 'Application Access Token'
  })

@api.route('/health/status', methods=['GET'])
def healthcheck():
  return jsonify({'status': 'UP'})


@api.route('/stats', methods=['DELETE'])
def resetStats():
  stats.clear()
  return '', 204


@api.route('/stats/<key>', methods=['GET'])
def getStats(key):
  return jsonify(stats.get(key, {"requests": 0, "events": 0}))


@api.route('/stats', methods=['GET'])
def getAllStats():
  return jsonify(stats)


def count_events(req):
  try:
    return len(req.get_json(force=True))
  except:
    print('error counting received events', file=sys.stderr)
    return 0


@api.route('/data/<key>', methods=['POST'])
def postData(key):
  auth_type = request.args.get('auth', '')

  if auth_type == 'oauth2' and 'Authorization' not in request.headers:
    return jsonify({'message': 'missing authorization header'}), 401

  if auth_type == 'oauth2' and request.headers['Authorization'] != f'Bearer {access_token}':
    return jsonify({'message': 'invalid bearer token in auth header'}), 401

  if key not in stats:
    stats[key] = {"requests": 0, "events": 0}

  stats[key]["requests"] += 1
  stats[key]["events"] += count_events(req)

  time.sleep(random.randint(0, 1000) / 1000.0)

  return jsonify({'message': 'ok'}), 200


if __name__ == '__main__':
    api.run(host='0.0.0.0', port=8084)

