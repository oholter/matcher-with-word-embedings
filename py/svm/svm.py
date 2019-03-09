from sklearn import svm
from sklearn import tree
import pandas as pd

input_file = "/home/ole/master/test_onto/res.txt"

anchors = [['http://cmt#Document', 'http://ekaw#Document'],
        ['http://cmt#ConferenceMember', 'http://ekaw#Conference_Participant'],
        ['http://cmt#Author', 'http://ekaw#Paper_Author'],
        ['http://cmt#Person', 'http://ekaw#Person'],
        ['http://cmt#Conference', 'http://ekaw#Conference'],
        ['http://cmt#Review', 'http://ekaw#Review'],
        ['http://cmt#Paper', 'http://ekaw#Paper'],
        ['http://cmt#PaperFullVersion', "http://ekaw#Regular_Paper"]]


data = pd.read_csv(input_file)
#print(data.head())
#print(data.columns)
#data.astype({"a" : str, "b" : str})
#test = data[(data.a =='http://cmt#Paper') & (data.b=='http://ekaw#Paper')]
#print(test)

#X = [[0,0,0,0,0], [1,1,2,2,2], [2,2,3,3,3], [3,3,1,1,1]]
#y = [0,1,3, 4]

X = data[['owl2vec', 'label', 'pretrained']]
#print(X)

Y = [0] * len(X)
true_values = []
num_ones = 0
for cmt, ekaw in anchors:
    try:
        i = data[(data.a == cmt) & (data.b == ekaw)].index.values[0]
        #print(i)
        Y[i] = 1
        num_ones += 1
    except IndexError:
        continue


print("number of trained: {}".format(num_ones))

print("svc:")
print("-----------")
clf = svm.SVC(gamma='scale')
clf.fit(X,Y)

pred = clf.predict(X)

for i in range(len(pred)):
    if pred[i] == 1:
        print("found: {} : {}".format(data.a.loc[[i]].values[0],
            data.b.loc[[i]].values[0]))


# testing decision tree
print("decision tree:")
print("-----------")
t = tree.DecisionTreeClassifier()
t = t.fit(X,Y)

pred = clf.predict(X)

for i in range(len(pred)):
    if pred[i] == 1:
        print("found: {} : {}".format(data.a.loc[[i]].values[0],
            data.b.loc[[i]].values[0]))
