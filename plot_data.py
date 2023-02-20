#import csv
import os
import numpy
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
from scipy.stats import spearmanr
from scipy.stats import ttest_rel
import math
import seaborn as sns

matplotlib.use('cairo')

pd.set_option('display.max_columns', None)
pd.set_option('display.max_rows', None)
pd.set_option('display.max_colwidth', None)

# Read and merge statistic files
#try:
#    with open("results/.current", "r") as f:
#        line = f.readline().strip()
#        dir_name = "results/" + line
#except OSError:
#    print ("Failed to read file .current")
#    os.exit(-1)

#dir_name = 'data/2023-02-10_23-14-58'
dir_name = 'data/2023-02-11_13-54-41'

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
df_all['correct'] = (df_all['InTime'] & df_all['Success'])
#df_all = df_all[df_all['InTime'] == True]
#df_all = df_all[df_all['Success'] == True]
df_all = df_all.drop(['AlgorithmIteration', 'InTime', 'Success'], axis=1)


#df_all = df_all[df_all['AlgorithmID'] != 0]

# Median of Algorithm Iterations
df_all = df_all.groupby(['ModelID', 'AlgorithmID', 'SystemIteration']).agg({'correct': 'max', 'Time': 'median', 'SampleSize': 'median', 'SampleSize': 'median'})
df_all = df_all.reset_index()

# Join Algorithm Names
key = ['AlgorithmID']
df_all = df_all.join(df_algo.set_index(key), on=key, rsuffix="_algo")
df_all['algo_name'] = df_all['Name'] + df_all['Settings']

# Join Model Names & Filter by Model
key = ['ModelID']
df_all = df_all.join(df_models.set_index(key), on=key, rsuffix="_model")
df_all = df_all[df_all['#Variables'] > 100]
#df_all = df_all.drop(['Name', '#Variables', '#Clauses'], axis=1)

# Normalize over System Iterations per Model
algo_gr = df_all[df_all['algo_name'] == 'YASAt2_m01']
algo_gr = algo_gr[['ModelID', 'SystemIteration', 'SampleSize', 'Time']]
key = ['ModelID', 'SystemIteration']
df_all = df_all.join(algo_gr.set_index(key), on=key, rsuffix="_algo")
df_all['complexity'] = df_all['#Variables']
df_all['AbsTime'] = df_all['Time'] / 1000
df_all['AbsSize'] = df_all['SampleSize'] / 1000
df_all['RelSize'] = df_all['SampleSize'] / df_all['SampleSize_algo']
df_all['RelTime'] = df_all['Time'] / df_all['Time_algo']

# Normalize over System Iterations per Model
#normalize = lambda x: (x / x.max())
#df_all['Time'] = df_all.groupby(['ModelID', 'SystemIteration'])['Time'].transform(normalize)
#df_all['SampleSize'] = df_all.groupby(['ModelID', 'SystemIteration'])['SampleSize'].transform(normalize)
#df_all = df_all.reset_index()

# Join Model Names
#key = ['ModelID']
#df_all = df_all.join(df_models.set_index(key), on=key, rsuffix="_model")

#df_all = df_all.drop(['ModelID', 'Name_model', 'AlgorithmID', 'Settings', '#Varaiables', '#Clauses'], axis=1)

# Plot time
#df_time = df_all[['algo_name', 'Time']]
#df_time.boxplot(by='algo_name')

def plotdata(df, yvar, name):
    #bp = df.boxplot(by='algo_name')
    sns.boxplot(data=df, x='algo_name', y=yvar)

    plt.gcf().set_size_inches(16, 9)
    plt.grid()
    plt.savefig(out_dir_name + name + ".pdf", format="pdf", dpi=600, bbox_inches='tight', pad_inches=0)
    plt.show()
    plt.close()

def plotdata2(df, yvar, name):
    #bp = df.boxplot(by='algo_name')
    sns.scatterplot(data=df, x='complexity', y=yvar, hue='algo_name')
    plt.yscale('log')

    plt.gcf().set_size_inches(16, 9)
    plt.grid()
    plt.savefig(out_dir_name + name + ".pdf", format="pdf", dpi=600, bbox_inches='tight', pad_inches=0)
    plt.show()
    plt.close()

def testdata(df, yvar, name):
    print('Paired T-Test for ' + name)
    classes = df['algo_name'].unique()
    df2 = df[['ModelID', 'SystemIteration', 'algo_name', 'correct', yvar]]
    key = ['ModelID', 'SystemIteration']
    for a in classes:
        df_a = df2[df2['algo_name'] == a]
        for b in classes:
            if a != b:
                df_b = df2[df2['algo_name'] == b]
                df_c = df_a.join(df_b.set_index(key), on=key, rsuffix="_2")
                df_c = df_c[((df_c['correct'] == True) & (df_c['correct_2'] == True))]
                stat, p = ttest_rel(df_c[yvar], df_c[yvar+'_2'])
                if p > 0.05:
                    print('no  -> ' + a + ' : ' + b + (' %.3f (%.3f)' % (stat, p)))
                #else:
                    #print('yes -> ' + a + ' : ' + b + (' %.3f (%.3f)' % (stat, p)))

#print(df_all)

testdata(df_all, 'AbsTime', 'time')
testdata(df_all, 'AbsSize', 'size')

df_all = df_all[df_all['correct'] == True]

plotdata(df_all, 'RelTime', 'time')
plotdata(df_all, 'RelSize', 'size')

plotdata2(df_all, 'AbsTime', 'time_abs')
plotdata2(df_all, 'AbsSize', 'size_abs')

df_all = df_all.rename(columns={"algo_name": "Algorithm", "Name_model": "Model"})
df_save = df_all[['Algorithm', 'Model', 'SystemIteration', 'AbsTime', 'AbsSize', 'RelSize', 'RelTime']]
df_save.to_csv(out_dir_name + '/data_cleaned.csv', index=False)  

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
#plt.show()
