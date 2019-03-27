from sklearn.cluster import DBSCAN
from gensim.models import KeyedVectors
from sklearn import metrics
import numpy as np
import matplotlib.pyplot as plt
import hdbscan
import matplotlib.pyplot as plt
from sklearn.decomposition import PCA
import copy

c = ['blue', 'green', 'red', 'cyan', 'magenta', 'yellow', 'black']

def colors(c):
    clrs = copy.copy(c)
    while True:
        if (len(clrs) == 1):
            clrs = copy.copy(c)
        yield clrs.pop()
    
    
def plot_data(data):
    pca = PCA(n_components=2)
    result = pca.fit_transform(data)
    plt.scatter(result[1:, 0], result[1:, 1], c=next(colors(c)))
    plt.show()
    
    

word_limit = 300

#model = KeyedVectors.load("model4.bin")
model = KeyedVectors.load_word2vec_format("/home/ole/master/test_onto/model.bin", binary=False)
#model = KeyedVectors.load("model2.bin")
#model = KeyedVectors.load("label.bin")
#model = KeyedVectors.load("label_pretrained.bin")
#w2v_vectors = model.wv.vectors # here you load vectors for each word in your model
w2v_vectors = model.wv.vectors
w2v_indices = {word: model.wv.vocab[word].index for word in model.wv.vocab} # here you load indices - with whom you can find an index of the particular word in your model

words = np.array(list(model.wv.vocab.keys()))
#print(words)
l = [model[w] for w in words]
#l = [model[w] for w in words if 'yeast' in w]
l = l[:word_limit]

cartesian_prod = np.transpose([np.tile(words, len(words)), np.repeat(words, len(words))])

l = [np.subtract(model[v], model[w]) for v, w in cartesian_prod]
l = l[:word_limit]
print(l)

plot_data(l)

array = np.array(l)
#print(array)
#print(cartesian_prod)

hdb = hdbscan.HDBSCAN(min_cluster_size=5, min_samples=1,
        gen_min_span_tree=True,
        cluster_selection_method='eom',
        #cluster_selection_method='leaf',
        allow_single_cluster=False)
cluster_labels = hdb.fit_predict(array)
#hdb.condensed_tree_.plot()
#plt.show()
#print(cluster_labels)
core_samples_mask = np.zeros_like(hdb.labels_, dtype=bool)
#core_samples_mask[hdb.core_sample_indices_] = True
labels = hdb.labels_
print(labels)
n_clusters_ = len(set(labels)) - (1 if -1 in labels else 0)
n_noise_ = list(labels).count(-1)

print('Estimated number of clusters: %d' % n_clusters_)
print('Estimated number of noise points: %d' % n_noise_)

unique_labels = set(labels)
print(unique_labels)


l2 = [ [] for i in range(n_clusters_) ]
#words_to_use = [w for w in words if 'yeast' in w]
words_to_use = [w for w in cartesian_prod]

for w, label in zip(words_to_use, labels):
    if label == -1:
        continue
    #print("{} is in cluster: {}".format(w, j))
    l2[label].append([a for a in w])

for liste in l2:
    print(liste)


    
