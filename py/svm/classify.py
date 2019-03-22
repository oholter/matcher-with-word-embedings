from sklearn import svm
from gensim.models import Word2Vec

input_model = '/home/ole/master/test_onto/model.bin'

anchors = [['document', 'document'],
        ['conference_participant', 'person'],
        ['paper_author', 'person'],
        ['person', 'person'],
        ['conference', 'conference'],
        ['review', 'document'],
        ['paper', 'document'],
        ['regular_paper', "document"],
        ['conference_trip', 'conference'],
        ['individual_presentation', 'conference']]

model = Word2Vec.load(input_model)
vectors = model.wv

anchor_list = [anchor[0] for anchor in anchors]
Y = [anchor[1] for anchor in anchors ]
X = [vectors.word_vec(anchor) for anchor in anchor_list]

clf = svm.SVC(gamma='scale')
clf.fit(X,Y)

vocab_list = [word for word in model.wv.vocab]
all_vectors = [vectors.word_vec(word) for word in vocab_list]


pred = clf.predict(all_vectors)

for i in range(len(pred)):
    print("prediction: {} of class {}".format(vocab_list[i], pred[i]))
    
print("vocab size: {}".format(len(all_vectors)))