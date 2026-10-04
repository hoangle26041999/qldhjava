import sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
path = "d:/LUMI-HOME/qldhjava/src/main/resources/templates/orders/create-multi.html"
data = open(path, "rb").read()
print("Size:", len(data))

text = data.decode("utf-8", errors="replace")
# Check mojibake
mojibake_signs = ["Ä", "Ă", "á»", "áº"]
for s in mojibake_signs:
    n = text.count(s)
    if n > 0:
        print("MOJIBAKE sign", repr(s), "found:", n)

# Check correct VI
for c in ["Hình", "Ä‘Æ¡n giÃ¡", "Ä", "Phá»¥ kiá»‡n", "ThÃ nh tiá»n", "Quy cÃ¡ch", "Sá»‘ lÆ°á»£ng"]:
    n = text.count(c)
    print("  " + repr(c) + ": " + str(n))
