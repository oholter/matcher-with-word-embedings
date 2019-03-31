from gensim.models import KeyedVectors
import numpy as np
from sklearn import preprocessing

input_model = "/home/ole/master/test_onto/model.bin"
word_limit = 10000


def prepare_data(input_model, word_limit):
    model = KeyedVectors.load_word2vec_format(input_model, binary=False)
    w2v_vectors = model.vocab
    w2v_indices = {word: model.wv.vocab[word].index for word in model.wv.vocab} # here you load indices - with whom you can find an index of the particular word in your model

    words = list(model.wv.vocab)
    words = words[:word_limit]
    vectors = [model[w] for w in words]
    vectors = np.array(vectors)
    return words, vectors, model


if __name__ == "__main__":
    words, vectors, model = prepare_data(input_model, word_limit)
    print(model.most_similar(positive=["http://ekaw#Accepted_Paper", "http://ekaw#Rejected_Paper"]))
    print(model.most_similar(positive=["http://ekaw#Workshop_Paper", "http://ekaw#Poster_Paper", "http://ekaw#Camera_Ready_Paper", "http://ekaw#Conference_Paper", "http://ekaw#Demo_Paper", "http://ekaw#Industrial_Paper", "http://ekaw#Regular_Paper"]))
    print(model.most_similar(positive=["http://ekaw#Regular_Session", "http://ekaw#Demo_Session", "http://ekaw#Workshop_Session", "http://ekaw#Poster_Session"]))
    print(model.most_similar(positive=["http://ekaw#Workshop_Paper", "http://ekaw#Regular_Session"], negative=["http://ekaw#Session"]))
