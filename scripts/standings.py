import os
import pandas as pd

base_dir = '../result/'

begin = 101
end = 200

df = []
names = []

for name in os.listdir(base_dir):
    path = base_dir + name
    csv = pd.read_csv(path, index_col='seed')
    index = csv.index
    if index[0] != begin or index[-1] != end:
        continue
    names.append(name)
    df.append(csv.score)

df = pd.concat(df, axis=1)
df.columns = names

dft = df.T
dfr = ((dft - dft.mean()) / dft.std()).T
print(dfr.sum())
