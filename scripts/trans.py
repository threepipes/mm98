import os

base_dir = '../results/'


for name in os.listdir(base_dir):
    path = base_dir + name
    data = ""
    with open(path) as f:
        for row in f:
            data += row.replace(" ", "")

    with open(path, 'w') as f:
        f.write(data)
