""" Extracts, dedupes, and outputs data to lines """

import json

# inputfile = 'health-data-staging-merged'
# outputfile = 'upload-list-uat'
# field = 'uploadId'

inputfile = 'stormpath-account-linked'
# outputfile = 'health-id-prod'
# field = 'p_healthId'
outputfile = 'health-id-staging'
field = 'healthId'

hcset = set()
with open(inputfile, 'r') as lines:
    records = [json.loads(line) for line in lines]
    for record in records:
        if field in record and record[field] is not None:
            hcset.add(record[field])

with open(outputfile, 'w') as outputfile:
    for hc in hcset:
        outputfile.write(hc)
        outputfile.write('\n')
