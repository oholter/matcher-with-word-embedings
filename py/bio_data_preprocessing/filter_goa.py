import pandas as pd

go_annotations_file = 'goa_yeast.gaf'
protein_interactions_file = 'yeast.protein.links.v11.0.txt'

data = pd.read_csv(go_annotations_file, skiprows=12, sep='\t', header=None)
data = data[data[6].str.contains('ND') | data[6].str.contains('IEA')]

data[[10,4]].to_csv('yeast_out.csv', index=False, header=False)
print(data[[10,4]].head())
data = pd.read_csv(protein_interactions_file, sep=' ', header=0)
print(data.head())