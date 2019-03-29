import gensim
from sklearn.decomposition import PCA
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

max_plots = 300
#max_plots = 5000
input_file = '/home/ole/master/test_onto/model.bin'
#input_file = 'model.bin'
#input_file = 'label.bin'
#input_file = 'label_pretrained2.bin'
#input_file = '/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp/out.txt'

#model = gensim.models.Word2Vec.load(input_file)
model = gensim.models.KeyedVectors.load_word2vec_format(input_file,
        binary=False)
pca = PCA(n_components=3)

wanted_words = ['http://ekaw#Person', 
                'http://ekaw#Student', 
                'http://ekaw#Paper_Author', 
                'http://ekaw#Document', 
                'http://ekaw#Paper', 
                'http://ekaw#University',
                'http://ekaw#Academic_Institution',
                'http://ekaw#Conference', 
                'http://ekaw#Scientific_Event']


word_list = wanted_words
#word_list = [word for word in model.wv.vocab if 'yeast' in word]
#print(word_list)
x = model[word_list]
x = x[:max_plots]
result = pca.fit_transform(x)
#print(result[:,0])
#print(result[:,1])


fig = plt.figure()
ax = Axes3D(fig)
ax.scatter(result[:, 0], result[:, 1], result[:, 2])

words = list(word_list)
for i, word in enumerate(words):
    if i >= len(result):
        break
    ax.text(result[i,0], result[i,1], result[i,2], word)


plt.show()
#principalDf = pd.DataFrame(data = principalComponents
             #, columns = ['principal component 1', 'principal component 2'])
