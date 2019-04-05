from gensim.models import Doc2Vec
import logging
import click


class LabeledLineSentence(object):
    def __init__(self, filename):
        self.filename = filename
    def __iter__(self):
        for uid, line in enumerate(open(filename)):
            yield LabeledSentence(words=line.split(), labels=['SENT_%s' % uid])


def read_input(input_file):
    """This method reads the input file format"""
    logging.info("reading file {0}...this may take a while".format(input_file))
    with open(input_file, 'r') as f:
        for i, line in enumerate(f):
            if (i % 10000 == 0):
                logging.info("read {0} reviews".format(i))
            # yield gensim.utils.simple_preprocess(line)
            yield [x.strip() for x in line.split(' ') if x.strip()]  


@click.command()
@click.argument("input_file", default="/home/ole/master/test_onto/walks_out.txt")
def main(input_file):
    sentence = LabeledSentence(words=[u'some', u'words', u'here'], labels=[u'SENT_1'])
    documents = list(read_input(input_file))
    model = Doc2Vec()
    model.build_vocab(sentences)
    model.train(1)

if __name__ == "__main__":
    main()