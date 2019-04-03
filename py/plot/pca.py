import gensim
from sklearn.decomposition import PCA
import matplotlib.pyplot as plt
import random

max_plots = 100
# max_plots = 5000
input_file = '/home/ole/master/test_onto/model.bin'
#input_file = '/home/ole/master/test_onto/tagspace.bin'
# input_file = 'label.bin'
# input_file = 'label_pretrained2.bin'
# input_file = '/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp/out.txt'

# model = gensim.models.Word2Vec.load(input_file)
model = gensim.models.KeyedVectors.load_word2vec_format(input_file,
        binary=False)
pca = PCA(n_components=2)

wanted_words = ['http://ekaw#Person', 
                'http://ekaw#Student', 
                'http://ekaw#Paper_Author', 
                'http://ekaw#Document', 
                'http://ekaw#Paper',
                'http://ekaw#Flyer',
                'http://ekaw#University',
                'http://ekaw#Academic_Institution',
                'http://ekaw#Conference',
                'http://ekaw#Scientific_Event']

wanted_words2 = ['Person',
                 'Student',
                 'Paper_Author',
                 'Document',
                 'Paper',
                 'Flyer',
                 'University',
                 'Academic_Institution',
                 'Conference',
                 'Scientific_Event']

word_list = model.vocab
#word_list = wanted_words
# word_list = [word for word in model.wv.vocab if 'yeast' in word]
# print(word_list)
x = model[word_list]
# random.shuffle(x)
result = pca.fit_transform(x)
result = result[:max_plots]
# print(result[:,0])
# print(result[:,1])

plt.scatter(result[:, 0], result[:, 1])

words = list(word_list)
for i, word in enumerate(words):
    if i >= len(result):
        break
    plt.annotate(word, xy=(result[i, 0], result[i, 1]))
    

plt.show()
# principalDf = pd.DataFrame(data = principalComponents
             # , columns = ['principal component 1', 'principal component 2'])
