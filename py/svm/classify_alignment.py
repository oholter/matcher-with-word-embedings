from sklearn import svm
from gensim.models import Word2Vec
import numpy as np

input_model = '/home/ole/master/test_onto/model.bin'

positive = [['document', 'document'],
        ['conference_member', 'conference_participant'],
        ['author', 'paper_author'],
        ['person', 'person'],
        ['conference', 'conference'],
        ['review', 'review'],
        ['paper', 'paper'],
        ['paper_full_version', "regular_paper"]]

negative = [['document', 'person'],
            ['conference_member', 'review'],
            ['author', 'regular_paper'],
            ['conference', 'session'],
            ['paper', 'document'],
            ['regular_paper', 'conference']]

model = Word2Vec.load(input_model)
vectors = model.wv


Y = [1] * len(positive)
Y.extend([0] * len(negative))
X = [model[w1] + model[w2] for w1, w2 in positive]
X.extend([model[w1] + model[w2] for w1, w2 in negative])
X = np.array(X)

#X.extend([[model[w1], model[w2]] for w1, w2 in negative])
print(X[1])

clf = svm.SVC()
clf.fit(X,Y)

vocab_list = [word for word in model.wv.vocab]
cartesian_prod = np.transpose([np.tile(vocab_list, len(vocab_list)), np.repeat(vocab_list, len(vocab_list))])
#print(cartesian_prod)
all_vectors = np.array([model[w1] + model[w2] for w1, w2 in cartesian_prod])


pred = clf.predict(all_vectors)

for i in range(len(pred)):
    if pred[i] == 1:
        print("prediction: {} of class {}".format(cartesian_prod[i], pred[i]))
    
print("vocab size: {}".format(len(all_vectors)))