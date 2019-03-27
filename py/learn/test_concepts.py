from gensim.models import Word2Vec
from gensim.models import KeyedVectors

input_file = '/home/ole/Downloads/GoogleNews-vectors-negative300.bin'

model = KeyedVectors.load_word2vec_format(input_file, binary=True)

sim1 = model.wv.most_similar(positive=['fridge', 'food'], negative=['cold'])
print(sim1)
