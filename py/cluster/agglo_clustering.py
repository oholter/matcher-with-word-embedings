from gensim.models import KeyedVectors
import matplotlib.pyplot as plt
import scipy.cluster.hierarchy as sch
from sklearn.cluster import AgglomerativeClustering
import numpy as np
from sklearn.decomposition import PCA
from functools import reduce



#input_model = "/home/ole/master/test_onto/model.bin"
input_model = '/home/ole/master/test_onto/model.bin'

word_limit = 10000
num_clusters = 7

def prepare_data(input_model, word_limit):
    model = KeyedVectors.load_word2vec_format(input_model, binary=False)
    w2v_vectors = model.vocab
    w2v_indices = {word: model.wv.vocab[word].index for word in model.wv.vocab} # here you load indices - with whom you can find an index of the particular word in your model

    words = list(model.wv.vocab)
    words = words[:word_limit]
    vectors = [model[w] for w in words]
    vectors = np.array(vectors)
    return words, vectors, model
    

def create_clusters(data):
    dendrogram = sch.dendrogram(sch.linkage(data, method='ward'))
    hc = AgglomerativeClustering(n_clusters=num_clusters, affinity = 'euclidean', linkage = 'ward')
    agglo = hc.fit_predict(data)
    return agglo

    
    
def print_clusters(agglo, data, model):
    clusters = [[] for i in range(max(agglo) + 1)]
    n = 0
    for clustno, word in zip(agglo, data):
        clusters[clustno].append(word)
        n +=1
    
    print("Tot: {} elem".format(n))
    
    n = 0
    for cluster in clusters:
        print("\nCluster {}".format(n))
        n += 1
        sum_vector = add_vectors([model[w] for w in cluster])
        central_concept = model.most_similar(positive=[sum_vector], topn=1)[0]
        print("Title: {}".format(central_concept[0]))
        for i in cluster:
            print(i)
    
    print("\nTot elements: {}".format(n))
    
    
def find_titles(agglo, words, model):
    clusters = [[] for i in range(max(agglo) + 1)]
    central_concepts = []
    n = 0
    for clustno, word in zip(agglo, words):
        clusters[clustno].append(word)
        n +=1
    
    n = 0
    for cluster in clusters:
        sum_vector = add_vectors([model[w] for w in cluster])
        central_concept = model.most_similar(positive=[sum_vector], topn=1)[0]
        central_concepts.append(central_concept)
    
    return np.array(central_concepts)[:,0]
    
    
    #todo: must fix plot
def plot_cluster_data(vectors, agglo, central_concepts):
    pca = PCA(n_components=2)
    result = pca.fit_transform(vectors)
    plt.clf()

    n = 0
    clusters = [[] for i in range(max(agglo) + 1)]
    for clustno, vector in zip(agglo, result):
        clusters[clustno].append(result[n])
        n += 1
    
    for i, cluster in enumerate(clusters):
        cluster = np.array(cluster)
        plt.scatter(cluster[:,0], cluster[:,1])
        concept = central_concepts[i]
        #concept = concept.split("#")[1]
        plt.annotate(concept, xy=(cluster[0,0], cluster[0,1]))
    plt.show()
    
def add_vectors(vectors):
    sum_vec = reduce(lambda x, y : x + y, vectors)
    #ave_vec = sum_vec / len(vectors)
    #return ave_vec
    return sum_vec        
            
if __name__ == "__main__":
    words, vectors, model = prepare_data(input_model, word_limit)
    agglo = create_clusters(vectors)
    print_clusters(agglo, words, model)
    titles = find_titles(agglo, words, model)
    plot_cluster_data(vectors, agglo, titles)
#    print_cluster_title(kmeans, vectors, model)
    
    