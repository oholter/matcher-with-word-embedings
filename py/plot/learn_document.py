import gensim
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

input_file = '/home/ole/workspace/MatcherWithWordEmbeddings/target/classes/temp/out/temp.txt'

def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            #yield gensim.utils.simple_preprocess(line)
            yield line.split(' ')


logging.info("reading input_file: {}".format(input_file))
documents = list(read_input(input_file))

model = gensim.models.Word2Vec(documents,size=150,window=10, min_count=1, workers=8, negative=20)

model.train(sentences=documents, total_examples=len(documents), epochs=500)
#model.wv.save_word2vec_format('model.bin', binary=True)
model.save("model.bin")