""" Extracts and dedupes health codes """

import json

inputfile = 'health-data-staging-merged'
hcset = set()
with open(inputfile, 'r') as lines:
    records = [json.loads(line) for line in lines]
    for record in records:
        hcset.add(record['healthCode'])

with open('health-code-staging', 'w') as outputfile:
    for hc in hcset:
        outputfile.write(hc)
        outputfile.write('\n')
