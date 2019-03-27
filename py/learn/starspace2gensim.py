input_file = "/home/ole/master/test_onto/cache/starspace.model.tsv"
output_file = "/home/ole/master/test_onto/model.bin"

with open(input_file, 'r') as inp, open(output_file, 'w') as outp:
    line_count = '...'    # line count of the tsv file (as string)
    dimensions = '...'    # vector size (as string)
    outp.write(' '.join([line_count, dimensions]) + '\n')
    for line in inp:
        words = line.strip().split()
        outp.write(' '.join(words) + '\n')