import json
import requests

url = 'https://dummyjson.com/products?limit=200'

with open('random-products-output.json', 'w') as file:
  content = []

  response = requests.request("GET", url).json()
  for p in response['products']:
    print(p['id'])
    obj = {
      'id': p['id'],
      'category': p['category'],
      'title': p['title'],
      'description': p['description'],
      'price': p['price'],
      'rating': p['rating'],
      'brand': None if 'brand' not in p else p['brand']
    }
    content.append(obj)
  
  file.write(json.dumps(content))

