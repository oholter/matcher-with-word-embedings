import gensim
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

input_file = \
    '/home/ole/master/test_onto/walks_out.txt'


def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            # yield gensim.utils.simple_preprocess(line)
            yield [x.strip() for x in line.split(' ') if x.strip()]  


logging.info("reading input_file: {}".format(input_file))
documents = list(read_input(input_file))

model = gensim.models.FastText(sg=1, hs=0, size=100, workers=8, word_ngrams=1, 
                               min_n=3, max_n=6, sentences=documents, window=20, min_count=1, iter=5, negative=25)
model.wv.save_word2vec_format("/home/ole/master/test_onto/model.bin", binary=False)
# model.save("/home/ole/master/test_onto/model3.bin")
