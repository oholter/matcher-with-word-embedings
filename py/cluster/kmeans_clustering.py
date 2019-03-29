from gensim.models import KeyedVectors
import matplotlib.pyplot as plt
from sklearn.cluster import KMeans
import numpy as np
from sklearn.decomposition import PCA
from functools import reduce



input_model = "/home/ole/master/test_onto/model.bin"
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
    kmeans = KMeans(n_clusters=num_clusters).fit(data)
    return kmeans


    
    
def print_clusters(kmeans, data, model):
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
        sum_vector = add_vectors([model[w] for w in clusters[item]])
        central_concept = model.most_similar(positive=[sum_vector], topn=1)[0]
        print("Title: {}".format(central_concept[0]))
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
    
def add_vectors(vectors):
    sum_vec = reduce(lambda x, y : x + y, vectors)
    #ave_vec = sum_vec / len(vectors)
    #return ave_vec
    return sum_vec        
            
if __name__ == "__main__":
    words, vectors, model = prepare_data(input_model, word_limit)
    kmeans = create_clusters(vectors)
    print_clusters(kmeans, words, model)
    #plot_cluster_data(vectors, kmeans)
    