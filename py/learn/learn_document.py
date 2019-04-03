import gensim
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

input_file = '/home/ole/master/test_onto/walks_out.txt'
output_file = '/home/ole/master/test_onto/model.bin'


def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            # yield gensim.utils.simple_preprocess(line, deacc=False, min_len=2, max_len=15)
            # excluding empty strings cannot use simple_preprocess bc this will alter uris
            yield [x.strip() for x in line.split(' ') if x.strip()]  


logging.info("reading input_file: {}".format(input_file))
documents = list(read_input(input_file))

model = gensim.models.Word2Vec(documents, size=100, window=10, min_count=1,
        workers=12, iter=10, sg=1, hs=0, negative=25)
model.wv.save_word2vec_format(output_file, binary=False)
#model.save("model.bin")
