import setuptools

with open("README.md", "r", encoding="utf-8") as fh:
    long_description = fh.read()

setuptools.setup(
    name="acrome-smd",
    version="2.0.2",
    author="Furkan Kırlangıç",
    author_email="furkankirlangic@acrome.net",
    description="Python library for interfacing with Acrome Smart Motor Drivers (SMD) products.",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/Acrome-Smart-Motor-Driver/python-library",
    project_urls={
        "Bug Tracker": "https://github.com/Acrome-Smart-Motor-Driver/python-library/issues",
        },
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: GNU General Public License v3 (GPLv3)",
        "Operating System :: OS Independent",
    ],
    packages=setuptools.find_packages(exclude=['tests', 'test']),
    install_requires=["pyserial", "stm32loader", "crccheck", "requests", "packaging"],
    python_requires=">=3.7"
)
