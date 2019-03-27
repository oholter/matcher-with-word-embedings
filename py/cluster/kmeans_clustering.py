from gensim.models import KeyedVectors
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
import numpy as np
from sklearn.decomposition import PCA



input_model = "/home/ole/master/test_onto/model.bin"
word_limit = 300
num_clusters = 10

def prepare_data(input_model, word_limit):
    model = KeyedVectors.load_word2vec_format(input_model, binary=False)
    w2v_vectors = model.vocab
    w2v_indices = {word: model.wv.vocab[word].index for word in model.wv.vocab} # here you load indices - with whom you can find an index of the particular word in your model

    words = list(model.wv.vocab)
    words = words[:word_limit]
    vectors = [model[w] for w in words]
    vectors = np.array(vectors)
    return words, vectors
    

def create_clusters(data):
    kmeans = KMeans(n_clusters=num_clusters).fit(data)
    return kmeans
    
    
def print_clusters(kmeans, data):
    labels = kmeans.labels_
    clusters = {}
    n = 0
    for item in labels:
        if item in clusters:
            clusters[item].append(data[n])
        else:
            clusters[item] = [data[n]]
        n +=1
    
    n = 0
    for item in clusters:
        print("\nCluster {}".format(item))
        for i in clusters[item]:
            n += 1
            print(i)
    
    print("\nTot elements: {}".format(n))
    
    
    #todo: must fix plot
def plot_cluster_data(vectors, kmeans):
    pca = PCA(n_components=2)
    result = pca.fit_transform(vectors)

    labels = kmeans.labels_
    clusters = {}
    n = 0
    for item in labels:
        if item in clusters:
            clusters[item].append(result[n])
        else:
            clusters[item] = [result[n]]
        n +=1
    
    n = 0
    for item in clusters:
        cluster = clusters[item]
        print(cluster)
        print("\nCluster {}".format(cluster[:,0]))

        plt.scatter(cluster[:,0], cluster[:,1])
    plt.show()
            
            
if __name__ == "__main__":
    words, vectors = prepare_data(input_model, word_limit)
    clusters = create_clusters(vectors)
    print_clusters(clusters, words)
    #plot_cluster_data(vectors, clusters)
    
    