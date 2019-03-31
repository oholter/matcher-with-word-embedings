from sklearn import svm
import numpy as np
import gensim
import math
import matplotlib.pyplot as plt
from sklearn.decomposition import PCA
from sklearn.metrics import classification_report


input_file = '/home/ole/master/test_onto/model.bin'

positive = [['review', 'document'],
            ['positive_review', 'document'],
            ['neutral_review', 'document'],
            ['negative_review', 'document'],
            ['pc_member', 'person'],
            ['sc_member', 'person'],
            ['conference_participant', 'person'],
            ['late_registered_participant', 'person'],
            ['early_registered_participant', 'person'],
            ['demo_chair', 'person'],
            ['session_chair', 'person'],
            ['evaluated_paper', 'document'],
            ['assigned_paper', 'document'],
            ['rejected_paper', 'document'],
            ['paper', 'document'],
            ['submitted_paper', 'document'],
            ['camera_ready_paper', 'document'],
            ['conference_paper', 'document'],
            ['possible_reviewer', 'person'],
            ['multi_author_volume', 'document'],
            ['flyer', 'document'],
            ['programme_brochure', 'document'],
            ['person', 'person'],
            ['document', 'document'],
            ['paper', 'paper']
            ]

negative = [
            ['person', 'scientific_event'],
            ['conference_participant', 'scientific_event'],
            ['document', 'scientific_event'],
            ['conference_participant', 'scientific_event'],
            ['organisation', 'person'],
            ['organising_agency', 'person'],
            ['proceedings_publisher', 'person'],
            ['academic_institution', 'person'],
            ['research_institute', 'person'],
            ['university', 'person'],
            ['organisation', 'document'],
            ['organising_agency', 'document'],
            ['proceedings_publisher', 'document'],
            ['organisation', 'paper'],
            ['organising_agency', 'paper'],
            ['proceedings_publisher', 'paper'],
            ['person', 'paper'],
            ['student', 'paper'],
            ['external_reviewer', 'paper'],
            ['paper_author', 'paper'],
            ['agency_staff_member', 'paper'],
            ['document', 'person'],
            ['paper', 'person'],
            ['person', 'document'],
            ['person', 'paper']
            ]



def plot_training_data(data):
    pca = PCA(n_components=2)
    result = pca.fit_transform(data)
    plt.scatter(result[:, 0], result[:, 1], c="red")
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
all_samples = list(positive)
all_samples.extend(list(negative))
X = [np.subtract(model[w2],model[w1]) for w1, w2 in positive]
X.extend([np.subtract(model[w2],model[w1]) for w1, w2 in negative])

#X.extend([[model[w1], model[w2]] for w1, w2 in negative])
print(X[7])

clf = svm.SVC(kernel='rbf')
clf.fit(X,Y)

vocab_list = [word for word in model.wv.vocab]
cartesian_prod = np.transpose([np.tile(vocab_list, len(vocab_list)), np.repeat(vocab_list, len(vocab_list))])
#print(cartesian_prod)
all_vectors = np.array([np.subtract(model[w2],model[w1]) for w1, w2 in cartesian_prod])

pred = clf.predict(all_vectors)
#pred = clf.predict(X)

all_samples = positive
all_samples.extend(negative)
print_pred(pred, cartesian_prod, 1000)
#print_pred(pred, all_samples, 1000)


print(classification_report(Y, pred))
#plot_training_data(X)
