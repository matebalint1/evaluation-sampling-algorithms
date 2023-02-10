#import csv
import os
import numpy
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
from scipy.stats import spearmanr
import scipy.stats as test
import math
import seaborn as sns

#matplotlib.use('cairo')

pd.set_option('display.max_columns', None)
pd.set_option('display.max_rows', None)
pd.set_option('display.max_colwidth', None)

# Read and merge statistic files
try:
    with open("results/.current", "r") as f:
        line = f.readline().strip()
        dir_name = "results/" + line
except OSError:
    print ("Failed to read file .current")
    os.exit(-1)

print(dir_name)
df_data = pd.read_csv(dir_name + "/data/data.csv", sep = ';')
df_algo = pd.read_csv(dir_name + "/data/algorithms.csv", sep = ';')
df_models = pd.read_csv(dir_name + "/data/models.csv", sep = ';')

out_dir_name = dir_name + '/plot/'
print(out_dir_name)

# Create output directory
if not os.path.exists(out_dir_name):
    try:
        os.mkdir(out_dir_name)
    except OSError:
        print ("Failed to create output directory %s" % output_path)
        os.exit(-1)

# Columns

df_all = df_data
print(list(df_all.columns))

# Filter
df_all = df_all[df_all['InTime'] == True]
df_all = df_all[df_all['Success'] == True]
df_all = df_all.drop(['AlgorithmIteration', 'InTime', 'Success'], axis=1)


# Join Model Names & Filter by Model
key = ['ModelID']
df_all = df_all.join(df_models.set_index(key), on=key, rsuffix="_model")
df_all = df_all[df_all['#Varaiables'] > 1000]
df_all = df_all.drop(['Name', '#Varaiables', '#Clauses'], axis=1)

# Median of Algorithm Iterations
df_all = df_all.groupby(['ModelID', 'AlgorithmID', 'SystemIteration']).agg({'Time': 'median', 'SampleSize': 'median'})
df_all = df_all.reset_index()


algo_gr = df_all[df_all['AlgorithmID'] == 1]
algo_gr = algo_gr[['ModelID', 'SystemIteration', 'SampleSize', 'Time']]
key = ['ModelID', 'SystemIteration']
df_all = df_all.join(algo_gr.set_index(key), on=key, rsuffix="_algo")
df_all['SampleSize'] = df_all['SampleSize'] / df_all['SampleSize_algo']
df_all['Time'] = df_all['Time'] / df_all['Time_algo']

# Normalize over System Iterations per Model
#normalize = lambda x: (x / x.max())
#df_all['Time'] = df_all.groupby(['ModelID', 'SystemIteration'])['Time'].transform(normalize)
#df_all['SampleSize'] = df_all.groupby(['ModelID', 'SystemIteration'])['SampleSize'].transform(normalize)
#df_all = df_all.reset_index()

# Join Algorithm Names
key = ['AlgorithmID']
df_all = df_all.join(df_algo.set_index(key), on=key, rsuffix="_algo")
df_all['algo_name'] = df_all['Name'] + df_all['Settings']

# Join Model Names
#key = ['ModelID']
#df_all = df_all.join(df_models.set_index(key), on=key, rsuffix="_model")

#df_all = df_all.drop(['ModelID', 'Name_model', 'AlgorithmID', 'Settings', '#Varaiables', '#Clauses'], axis=1)

# Plot time
df_time = df_all[['algo_name', 'Time']]
df_time.boxplot(by='algo_name')

# Plot size
df_size = df_all[['algo_name', 'SampleSize']]
df_size.boxplot(by='algo_name')

#df_all = df_all.groupby(['ModelID', 'AlgorithmID', 'SystemIteration'])

print(df_time)


#print(df_all)

#df_all = df_all[df_all["AlgorithmID"]==1]
#fig, axes = plt.subplots()
#axes.violinplot(dataset=[df_all["Positive_count_mean"],
#                         df_all["Negative_count_mean"],
#                         df_all["Hamming_distance_mean"],
#                         df_all["fm_distribution_mean"]])

#axes.set_title('Title')
#axes.yaxis.grid(True)
#axes.set_xlabel('X')
#axes.set_ylabel('Y')
plt.show()
