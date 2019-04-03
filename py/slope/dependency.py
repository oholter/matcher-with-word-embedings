import numpy as np
import scipy.stats as stats
x = np.array([1, 0.8, 0.6, 0.4, 0.2, 0])

# EKAW-EKAW
#y = np.array([0.32, 0.33, 0.36, 0.31, 0.2, 0.02]) #owl2vec best
#y = np.array([0.68, 0.66, 0.73, 0.61, 0.4, 0.02]) #owl2vec disambiguate
#y = np.array([0.90, 0.93, 0.93, 0.85, 0.69, 0.13]) #synonyms best
#y = np.array([0.93, 0.94, 0.93, 0.83, 0.66, 0.16]) #synonyms disambiguate
#y = np.array([0.92, 0.92, 0.92, 0.83, 0.67, 0.14]) #synonyms all relations
#y = np.array([0.90, 0.90, 0.77, 0.49, 0.23, 0.01]) #synonyms translation m
#y = np.array([0.12, 0.12, 0.14, 0.12, 0.12, 0.13]) #rdf2vec best
#y = np.array([0.77, 0.70, 0.70, 0.68, 0.46, 0.04]) #two documents
#y = np.array([0.89, 0.89, 0.66, 0.51, 0.31, 0.0]) # secondorder
#y = np.array([0.60, 0.69, 0.68, 0.65, 0.48, 0.04]) # subclass


# EKAW-CMT
#y = np.array([0.68, 0.50, 0.33, 0.25, 0.25, 0.10]) #owl2vec best
#y = np.array([0.68, 0.50, 0.33, 0.25, 0.25, 0.10]) #owl2vec disambiguate
#y = np.array([0.65, 0.43, 0.28, 0.13, 0.15, 0.15]) #synonyms best
#y = np.array([0.55, 0.43, 0.28, 0.15, 0.13, 0.15]) #synonyms disambiguate
#y = np.array([0.55, 0.43, 0.38, 0.18, 0.13, 0.15]) #synonyms all relations
#y = np.array([1.00, 0.75, 0.63, 0.38, 0.38, 0.13]) #synonyms translation m
#y = np.array([0.05, 0.13, 0.05, 0.06, 0.10, 0.04]) #rdf2vec best
#y = np.array([0.58, 0.50, 0.38, 0.18, 0.20, 0.23]) #two documents
#y = np.array([1.00, 0.75, 0.75, 0.25, 0.13, 0.0]) # secondorder
y = np.array([0.95, 0.80, 0.75, 0.53, 0.40, 0.23]) # subclass


# ANATOMY
#y = np.array([0.69, 0.56, 0.44, 0.28, 0.14, 0.00]) #synonyms best
#y = np.array([0.69, 0.62, 0.56, 0.45, 0.33, 0.14]) #synonyms best



sx = x.sum()
sy = y.sum()
n = len(x)
xy = x*y
xx = x*x
yy = y*y
sxy = xy.sum()
sxx = xx.sum()
syy = yy.sum()
#print(yy)

b = ((n * sxy) - (sx*sy)) / ((n * sxx) - (sx * sx))

a = ((sy * sxx) - (sx * sxy)) / ((n * sxx) - (sx * sx))

print("f(x) = {} + {}x".format(a,b))
corr, _ = stats.pearsonr(x,y)
print("pearson's correlation: {}".format(corr))
