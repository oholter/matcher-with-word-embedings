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
    
    word_list = [
        "http://ekaw#Accepted_Paper",
        "http://ekaw#Workshop_Paper",
        "http://ekaw#Regular_Session",
        "http://ekaw#Paper",
        "http://ekaw#Academic_Institution",
        "http://ekaw#Conference_Participant",
        "http://ekaw#Review",
        "http://ekaw#Workshop",
        "http://ekaw#Individual_Presentation",
        "http://ekaw#Flyer",
        "http://ekaw#presentationOfPaper"
        ]
    
    word_list2 = [
        "Accepted_Paper",
        "Workshop_Paper",
        "Regular_Session",
        "Paper",
        "Academic_Institution",
        "Conference_Participant",
        "Review",
        "Workshop",
        "Individual_Presentation",
        "Flyer",
        "presentationOfPaper"
        ] 
    
    words, vectors, model = prepare_data(input_model, word_limit)
    for word in word_list:
    #for word in word_list:
        print("Most similar to: {} = {}".format(word, model.most_similar(positive=[word], topn=1)))
