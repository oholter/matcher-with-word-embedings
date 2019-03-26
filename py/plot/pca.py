import gensim
from sklearn.decomposition import PCA
import matplotlib.pyplot as plt

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
pca = PCA(n_components=2)


word_list = model.vocab
#word_list = [word for word in model.wv.vocab if 'yeast' in word]
#print(word_list)
x = model[word_list]
result = pca.fit_transform(x)
result = result[:max_plots]
#print(result[:,0])
#print(result[:,1])

plt.scatter(result[:, 0], result[:, 1])

words = list(word_list)
for i, word in enumerate(words):
    if i >= len(result):
        break
    plt.annotate(word, xy=(result[i, 0], result[i, 1]))


plt.show()
#principalDf = pd.DataFrame(data = principalComponents
             #, columns = ['principal component 1', 'principal component 2'])
