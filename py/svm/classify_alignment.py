from sklearn import svm
import numpy as np
import gensim
import math
import matplotlib.pyplot as plt
from sklearn.decomposition import PCA
from sklearn.metrics import classification_report


input_file = '/home/ole/master/test_onto/model.bin'

positive = [['document', 'document'],
        ['conference_member', 'conference_participant'],
        ['author', 'paper_author'],
        ['person', 'person'],
        ['conference', 'conference'],
        ['review', 'review']]
        #['paper', 'paper'],
        #['paper_full_version', "regular_paper"]]

negative = [['document', 'person'],
            ['conference_member', 'review'],
            ['author', 'regular_paper'],
            ['conference', 'session'],
            ['paper', 'document'],
            ['regular_paper', 'conference']]



def plot_training_data(data):
    pca = PCA(n_components=2)
    result = pca.fit_transform(data)
    pos = result[:8]
    neg = result[8:]
    
    
    plt.scatter(pos[:, 0], pos[:, 1], c="blue")
    plt.scatter(neg[:, 0], neg[:, 1], c="red")
    plt.show()


def print_pred(pred, vocab, maxlen):
    for i in range(min(len(pred), maxlen)):
        print("prediction: {} of class {}".format(vocab[i], pred[i]))
    
    print("vocab size: {}".format(len(all_vectors)))


model = gensim.models.KeyedVectors.load_word2vec_format(input_file,
        binary=False)
vectors = model.vocab


Y = [1] * len(positive)
Y.extend([0] * len(negative))
X = [np.add(model[w1],model[w2]) for w1, w2 in positive]
X.extend([np.add(model[w1],model[w2]) for w1, w2 in negative])
#X = np.array(X)

#X.extend([[model[w1], model[w2]] for w1, w2 in negative])
print(X[7])

clf = svm.SVC(kernel='sigmoid')
clf.fit(X,Y)

vocab_list = [word for word in model.wv.vocab]
cartesian_prod = np.transpose([np.tile(vocab_list, len(vocab_list)), np.repeat(vocab_list, len(vocab_list))])
#print(cartesian_prod)
all_vectors = np.array([model[w1] + model[w2] for w1, w2 in cartesian_prod])

#pred = clf.predict(all_vectors)
pred = clf.predict(all_vectors)

all_samples = positive
all_samples.extend(negative)
print_pred(pred, cartesian_prod, 100)


print(classification_report(Y, pred))
plot_training_data(X)
