import gensim
import logging
from gensim.models import Word2Vec
from gensim.models import KeyedVectors


logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

walks_document = '/home/ole/master/test_onto/labels_out.txt'
output_model = '/home/ole/master/test_onto/label.bin'

def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            yield gensim.utils.simple_preprocess(line)
            #yield line.split(' ')


logging.info("reading input_file: {}".format(walks_document))
documents = list(read_input(walks_document))

model = Word2Vec(documents, size=100, min_count=1, window=20, workers=12, iter=50)
model.wv.save_word2vec_format(output_model, binary=False)

#model.save("label_pretrained.bin")