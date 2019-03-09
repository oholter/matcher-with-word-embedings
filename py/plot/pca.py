import gensim
from sklearn.decomposition import PCA
import matplotlib.pyplot as plt

max_plots = 500
#input_file = 'model.bin'
#input_file = 'label.bin'
input_file = 'label_pretrained.bin'
#input_file = '/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp/out.txt'

model = gensim.models.Word2Vec.load(input_file)
pca = PCA(n_components=2)

x = model[model.wv.vocab]
result = pca.fit_transform(x)
result = result[:max_plots]
print(result[:,0])
print(result[:,1])

plt.scatter(result[:, 0], result[:, 1])

words = list(model.wv.vocab)
for i, word in enumerate(words):
    if i >= len(result):
        break
    plt.annotate(word, xy=(result[i, 0], result[i, 1]))


plt.show()
#principalDf = pd.DataFrame(data = principalComponents
             #, columns = ['principal component 1', 'principal component 2'])
