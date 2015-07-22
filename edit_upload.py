import json

sid2c = {}
with open('health-id2code-staging', 'r') as lines:
    for line in lines:
        id2c = json.loads(line)
        sid2c[id2c['hid']] = id2c['hc']
print(sid2c['148d6079-7202-4615-892d-a60214e90bca'])

pid2c = {}
with open('health-id2code-prod', 'r') as lines:
    for line in lines:
        id2c = json.loads(line)
        pid2c[id2c['hid']] = id2c['hc']
print(pid2c['770568a4-2c5e-4bc9-9a62-4ec6f9fa289a'])

hcmap = {}
with open('stormpath-account-linked', 'r') as lines:
    for line in lines:
        acct = json.loads(line)
        if 'healthId' in acct:
            sid = acct['healthId']
            pid = acct['p_healthId']
            if sid is not None and pid is not None:
                sc = sid2c[sid]
                pc = pid2c[pid]
                if sc is not None and pc is not None:
                    hcmap[sc] = pc
print(len(hcmap))

uploads = []
with open('upload-data-staging', 'r') as lines:
    for line in lines:
        upload = json.loads(line)
        hc = upload['healthCode']
        if hc is not None and hc in hcmap:
            upload['healthCode'] = hcmap[hc]
            upload['status'] = 'VALIDATION_IN_PROGRESS'
            upload['validationMessageList'] = []
            uploads.append(upload)

with open('upload-data-staging-edited', 'w') as outputfile:
    for upload in uploads:
        outputfile.write(json.dumps(upload))
        outputfile.write('\n')
