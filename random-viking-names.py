import requests
import time

url = "https://asgardalaska.org/wp-admin/admin-ajax.php"
payload = 'action=generate_viking_name&lucky=generate'
headers = {
  'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
}

with open('random-vikig-names-output.txt', 'a') as file:
  for i in range(1000):

    response = requests.request("POST", url, headers=headers, data=payload)
    if not(200 <= response.status_code <= 299):
      continue

    name = response.json()['name']
    print(name)
    file.write(f'{name}\n')
    time.sleep(0.5)
