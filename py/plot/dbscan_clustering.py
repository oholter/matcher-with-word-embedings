from sklearn.cluster import DBSCAN
from gensim.models import KeyedVectors
from sklearn import metrics
import numpy as np
import matplotlib.pyplot as plt


model = KeyedVectors.load("model.bin")
w2v_vectors = model.wv.vectors # here you load vectors for each word in your model
w2v_indices = {word: model.wv.vocab[word].index for word in model.wv.vocab} # here you load indices - with whom you can find an index of the particular word in your model

def vectorize(line):
    words = []
    for word in line: # line - iterable, for example list of tokens
        try:
            w2v_idx = w2v_indices[word]
        except KeyError: # if you does not have a vector for this word in your w2v model, continue
            continue
        words.append(w2v_vectors[w2v_idx])
        if words:
            words = np.asarray(words)
            min_vec = words.min(axis=0)
            max_vec = words.max(axis=0)
            return np.concatenate((min_vec, max_vec))
        if not words:
            return None

words = model.wv.vocab
l = []
for w in words:
    l.append(model[w])

array = np.array(l)

db = DBSCAN(metric='cosine', eps=0.6, min_samples=2) # you can change these parameters, given just for example
cluster_labels = db.fit_predict(array) # where X - is your matrix, where each row corresponds to one document (line) from the docs, you need to cluster
#print(cluster_labels)
core_samples_mask = np.zeros_like(db.labels_, dtype=bool)
core_samples_mask[db.core_sample_indices_] = True
labels = db.labels_
print(labels)
n_clusters_ = len(set(labels)) - (1 if -1 in labels else 0)
n_noise_ = list(labels).count(-1)

print('Estimated number of clusters: %d' % n_clusters_)
print('Estimated number of noise points: %d' % n_noise_)

unique_labels = set(labels)
print(unique_labels)


l = [ [] for i in range(n_clusters_) ]

for w, label in zip(words, labels):
    if label == -1:
        continue
    #print("{} is in cluster: {}".format(w, j))
    l[label].append(w)

for liste in l:
    print(liste)
