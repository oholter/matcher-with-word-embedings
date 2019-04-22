import numpy as np
import scipy.stats as stats
x = np.array([1, 0.8, 0.6, 0.4, 0.2, 0])

# EKAW-EKAW
#y = np.array([0.75, 0.65, 0.48, 0.31, 0.12, 0.01]) #struc best
#y = np.array([1.0, 0.97, 0.84, 0.62, 0.34, 0.03]) #struc disambiguate
#y = np.array([0.81, 0.75, 0.61, 0.42, 0.29, 0.13]) #synonyms best
#y = np.array([0.97, 0.91, 0.85, 0.68, 0.62, 0.34]) #synonyms disambiguate
#y = np.array([0.92, 0.82, 0.72, 0.52, 0.29, 0.15]) #synonyms all relations
#y = np.array([0.90, 0.90, 0.77, 0.49, 0.23, 0.01]) #synonyms translation m
#y = np.array([0.63, 0.60, 0.44, 0.27, 0.14, 0.0]) #rdf2vec best
#y = np.array([0.48, 0.47, 0.52, 0.56, 0.56, 0.29]) #two documents
#y = np.array([0.87, 0.79, 0.66, 0.41, 0.21, 0.01]) # secondorder
#y = np.array([0.60, 0.69, 0.68, 0.65, 0.48, 0.04]) # subclass


# EKAW-CMT
#y = np.array([0.60, 0.48, 0.30, 0.23, 0.10, 0.00]) #struc best
#y = np.array([0.85, 0.70, 0.53, 0.43, 0.33, 0.08]) #struc disambiguate
#y = np.array([0.45, 0.33, 0.20, 0.23, 0.05, 0.05]) #synonyms best
#y = np.array([0.68, 0.65, 0.45, 0.23, 0.15, 0.05]) #synonyms disambiguate
#y = np.array([0.40, 0.30, 0.33, 0.18, 0.18, 0.03]) #synonyms all relations
#y = np.array([1.00, 0.75, 0.63, 0.38, 0.38, 0.13]) #synonyms translation m
#y = np.array([0.13, 0.0, 0.0, 0.0, 0.0, 0.0]) #rdf2vec best
#y = np.array([0.75, 0.58, 0.70, 0.70, 0.63, 0.20]) #two documents
#y = np.array([0.98, 0.78, 0.55, 0.50, 0.23, 0.03]) # secondorder
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
